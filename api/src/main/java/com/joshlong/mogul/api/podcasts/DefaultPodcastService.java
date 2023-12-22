package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulCreatedEvent;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.managedfiles.CommonMediaTypes;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.managedfiles.ManagedFileUpdatedEvent;
import com.joshlong.mogul.api.podcasts.production.MediaNormalizer;
import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.BiConsumer;

@Service
@Transactional
class DefaultPodcastService implements PodcastService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final MediaNormalizer mediaNormalizer;

	private final JdbcClient db;

	private final EpisodeRowMapper episodeRowMapper;

	private final ManagedFileService managedFileService;

	private final MogulService mogulService;

	private final ApplicationEventPublisher publisher;

	DefaultPodcastService(MediaNormalizer mediaNormalizer, MogulService mogulService, JdbcClient db,
			ManagedFileService managedFileService, ApplicationEventPublisher publisher) {

		this.db = db;
		this.mediaNormalizer = mediaNormalizer;
		this.mogulService = mogulService;
		this.managedFileService = managedFileService;
		this.episodeRowMapper = new EpisodeRowMapper(this::getPodcastById, id -> {
			if (id == null || id <= 0)
				return null;
			return managedFileService.getManagedFile(id);
		});
		this.publisher = publisher;
	}

	private Episode findUpdatedEpisodeForManagedFile(ManagedFile managedFile) {
		var id = managedFile.id();
		var allEpisodes = this.db//
			.sql("select  * from podcast_episode where interview =  ? or introduction = ?  or graphic = ?")//
			.params(id, id, id)//
			.query(this.episodeRowMapper)//
			.list();
		if (!allEpisodes.isEmpty())
			return allEpisodes.getFirst();
		return null;
	}

	@ApplicationModuleListener
	void podcastEpisodeManagedFileUpdated(ManagedFileUpdatedEvent managedFileUpdatedEvent) {
		var episode = findUpdatedEpisodeForManagedFile(managedFileUpdatedEvent.managedFile());
		var idOfUpdatedManagedFile = managedFileUpdatedEvent.managedFile().id();
		if (episode == null) {
			log.warn("could not find a podcast episode where the managedFile is "
					+ managedFileUpdatedEvent.managedFile());
			return;
		}
		var mappings = Map.of("produced_interview", Objects.requireNonNull(episode).interview(), //
				"produced_graphic", Objects.requireNonNull(episode).graphic(), //
				"produced_introduction", Objects.requireNonNull(episode).introduction()//
		);
		for (var colName : mappings.keySet()) {
			var importantManagedFile = mappings.get(colName);
			if (importantManagedFile.id().equals(idOfUpdatedManagedFile)) {
				normalizeManagedFile(colName, importantManagedFile, episode);
			}
		}
		// make sure we have the latest state.
		episode = getEpisodeById(episode.id());
		var complete = true;
		for (var managedFile : new ManagedFile[] { episode.introduction(), //
				episode.producedIntroduction(), //
				episode.interview(), //
				episode.producedInterview(), //
				episode.graphic(), //
				episode.producedGraphic()//
		}) {
			var written = managedFile != null && managedFile.written();
			if (!written) {
				log.debug("oh no! " + managedFile
						+ " is either null or not written, so marking the whole episode as incomplete");
				complete = false;
				break;
			}
		}
		this.db.sql(" update podcast_episode set complete = ?  where id =? ")//
			.params(complete, episode.id())//
			.update();

		this.publisher.publishEvent(new PodcastEpisodeUpdatedEvent(getEpisodeById(episode.id())));

	}

	private void normalizeManagedFile(String colName, ManagedFile importantManagedFile, Episode episode) {
		var normalized = this.mediaNormalizer.normalize(importantManagedFile);
		log.debug("you just changed the " + colName + ", going to normalize it. "
				+ "the new normalized managed file is " + normalized.id() + " for podcast episode " + episode.id());
		var existingValue = db.sql("select " + colName + " from podcast_episode where id =? ")
			.params(episode.id())
			.query((rs, rowNum) -> rs.getLong(colName))
			.single();

		db.sql("update podcast_episode set " + colName + " = ?   where id =?")
			.params(normalized.id(), episode.id())
			.update();
		if (existingValue != 0) {
			this.managedFileService.deleteManagedFile(existingValue);
		}
	}

	@ApplicationModuleListener
	void podcastEpisodeUpdated(PodcastEpisodeUpdatedEvent updatedEvent) {
		if (updatedEvent.episode().complete()) {
			this.publisher.publishEvent(new PodcastEpisodeCompletedEvent(updatedEvent.episode()));
		}
	}

	@ApplicationModuleListener
	void mogulCreated(MogulCreatedEvent createdEvent) {
		var podcast = createPodcast(createdEvent.mogul().id(), createdEvent.mogul().username() + "'s Podcast");
		Assert.notNull(podcast,
				"there should be a newly created podcast associated with the mogul [" + createdEvent.mogul() + "]");
	}

	@Override
	public Collection<Podcast> getAllPodcastsByMogul(Long mogulId) {
		return this.db.sql("select * from podcast where mogul_id = ? order by created")
			.param(mogulId)
			.query(new PodcastRowMapper())
			.list();
	}

	@Override
	public Collection<Episode> getEpisodesByPodcast(Long podcastId) {
		var podcast = getPodcastById(podcastId);
		Assert.notNull(podcast, "the podcast with id [" + podcastId + "] is null");
		return this.db.sql("select * from podcast_episode where podcast_id =? order by created ")
			.param(podcastId)
			.query(this.episodeRowMapper)
			.list();
	}

	@Override
	public Podcast createPodcast(Long mogulId, String title) {
		var kh = new GeneratedKeyHolder();
		this.db
			.sql("insert into podcast (mogul_id, title) values (?,?) on conflict on constraint "
					+ " podcast_mogul_id_title_key do update set title =excluded.title")
			.params(mogulId, title)
			.update(kh);
		var id = JdbcUtils.getIdFromKeyHolder(kh);
		var podcast = getPodcastById(id.longValue());
		this.publisher.publishEvent(new PodcastCreatedEvent(podcast));
		return podcast;
	}

	@Override
	public Episode createPodcastEpisode(Long podcastId, String title, String description, ManagedFile graphic,
			ManagedFile introduction, ManagedFile interview, ManagedFile producedGraphic, ManagedFile producedIntro,
			ManagedFile producedInterview, ManagedFile producedAudio) {
		Assert.notNull(podcastId, "the podcast is null");
		Assert.hasText(title, "the title has no text");
		Assert.hasText(description, "the description has no text");
		Assert.notNull(graphic, "the graphic is null ");
		Assert.notNull(introduction, "the introduction is null ");
		Assert.notNull(producedAudio, "the produced audio is null ");
		Assert.notNull(interview, "the interview is null ");
		Assert.notNull(producedGraphic, "the produced graphic is null");
		Assert.notNull(producedInterview, "the produced interview is null");
		Assert.notNull(producedIntro, "the produced intro is null");
		var kh = new GeneratedKeyHolder();
		this.db.sql("""
					insert into podcast_episode(
						podcast_id,
						title,
						description,
						graphic ,
						introduction ,
						interview ,
						produced_graphic,
						produced_introduction,
						produced_interview,
						produced_audio
					)
					values (?,?,?,?,?,?,?,?,?,?)
				""")
			.params(podcastId, title, description, graphic.id(), introduction.id(), interview.id(), producedAudio.id())
			.update(kh);
		var id = JdbcUtils.getIdFromKeyHolder(kh);
		var ep = this.getEpisodeById(id.longValue());
		publisher.publishEvent(new PodcastEpisodeCreatedEvent(ep));
		return ep;
	}

	@Override
	public Episode getEpisodeById(Long episodeId) {
		var res = db.sql("select * from podcast_episode where id =?").param(episodeId).query(episodeRowMapper).list();
		return res.isEmpty() ? null : res.getFirst();
	}

	@Override
	public void deletePodcast(Long podcastId) {
		var podcast = getPodcastById(podcastId);

		for (var episode : getEpisodesByPodcast(podcastId)) {
			deletePodcastEpisode(episode.id());
		}
		db.sql("delete from podcast where id= ?").param(podcastId).update();

		publisher.publishEvent(new PodcastDeletedEvent(podcast));
	}

	@Override
	public void deletePodcastEpisode(Long episodeId) {
		var func = (BiConsumer<ManagedFile, Set<Long>>) (mf, ids) -> {
			if (mf != null)
				ids.add(mf.id());
		};

		var episode = getEpisodeById(episodeId);

		var ids = new HashSet<Long>();
		func.accept(episode.graphic(), ids);
		func.accept(episode.introduction(), ids);
		func.accept(episode.interview(), ids);
		func.accept(episode.producedAudio(), ids);
		func.accept(episode.producedGraphic(), ids);
		func.accept(episode.producedInterview(), ids);
		func.accept(episode.producedIntroduction(), ids);

		this.db.sql("delete from podcast_episode where id= ?").param(episode.id()).update();

		for (var mfId : ids)
			this.managedFileService.deleteManagedFile(mfId);

		publisher.publishEvent(new PodcastEpisodeDeletedEvent(episode));
	}

	@Override
	public Podcast getPodcastById(Long podcastId) {
		return db.sql("select * from podcast where id = ?").param(podcastId).query(new PodcastRowMapper()).single();
	}

	@Override
	public Episode createPodcastEpisodeDraft(Long currentMogulId, Long podcastId, String title, String description) {
		var uid = UUID.randomUUID().toString();
		var podcast = getPodcastById(podcastId);
		Assert.notNull(podcast, "the podcast is null!");
		var bucket = PodcastService.PODCAST_EPISODES_BUCKET;
		var image = managedFileService.createManagedFile(currentMogulId, bucket, uid, "", 0, CommonMediaTypes.BINARY);
		var intro = managedFileService.createManagedFile(currentMogulId, bucket, uid, "", 0, CommonMediaTypes.BINARY);
		var interview = managedFileService.createManagedFile(currentMogulId, bucket, uid, "", 0,
				CommonMediaTypes.BINARY);
		var producedGraphic = managedFileService.createManagedFile(currentMogulId, bucket, uid, "produced-graphic.jpg",
				0, CommonMediaTypes.JPG);
		var producedIntroduction = managedFileService.createManagedFile(currentMogulId, bucket, uid,
				"produced-intro.mp3", 0, CommonMediaTypes.MP3);
		var producedInterview = managedFileService.createManagedFile(currentMogulId, bucket, uid,
				"produced-interview.mp3", 0, CommonMediaTypes.MP3);
		var producedAudio = managedFileService.createManagedFile(currentMogulId, bucket, uid, "produced-audio.mp3", 0,
				CommonMediaTypes.MP3);
		return createPodcastEpisode(podcastId, title, description, image, intro, interview, producedGraphic,
				producedIntroduction, producedInterview, producedAudio);
	}

	@Override
	public Episode updatePodcastEpisodeDraft(Long episodeId, String title, String description) {
		Assert.notNull(episodeId, "the episode is null");
		Assert.hasText(title, "the title is null");
		Assert.hasText(description, "the description is null");
		var episode = getEpisodeById(episodeId);
		mogulService.assertAuthorizedMogul(episode.podcast().mogulId());
		db.sql("update podcast_episode set title =? , description =? where  id = ?")
			.params(title, description, episodeId)
			.update();
		var episodeById = getEpisodeById(episodeId);
		Assert.notNull(episodeById, "the result should not be null");
		publisher.publishEvent(new PodcastEpisodeUpdatedEvent(episodeById));
		return episodeById;
	}

	@Override
	public Episode writePodcastEpisodeProducedAudio(Long episodeId, Long managedFileId) {
		this.managedFileService.refreshManagedFile(managedFileId);
		this.db.sql("update podcast_episode set produced_audio_updated = NOW() where id =? ")
			.params(episodeId)
			.update();
		return this.getEpisodeById(episodeId);
	}

}
