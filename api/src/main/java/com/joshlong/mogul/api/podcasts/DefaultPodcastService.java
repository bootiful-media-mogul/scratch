package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulCreatedEvent;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

@Transactional
@Service
class DefaultPodcastService implements PodcastService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JdbcClient db;

	private final EpisodeRowMapper episodeRowMapper;

	private final ManagedFileService managedFileService;

	DefaultPodcastService(JdbcClient db, ManagedFileService managedFileService) {
		this.db = db;
		this.managedFileService = managedFileService;
		this.episodeRowMapper = new EpisodeRowMapper(this::getPodcastById, managedFileService::getManagedFile);
	}

	@ApplicationModuleListener
	void mogulCreated(MogulCreatedEvent createdEvent) {
		var podcast = createPodcast(createdEvent.mogul().id(), createdEvent.mogul().username() + "'s Podcast");
		Assert.notNull(podcast,
				"there should be a newly created podcast associated with the mogul [" + createdEvent.mogul() + "]");
	}

	@Override
	public Collection<Podcast> getAllPodcastsByMogul(Long mogulId) {
		return this.db.sql("select * from podcast where mogul_id =? order by created")
			.param(mogulId)
			.query(new PodcastRowMapper())
			.list();
	}

	@Override
	public Collection<Episode> getEpisodesByPodcast(Long podcastId) {
		var podcast = getPodcastById(podcastId);
		Assert.notNull(podcast, "the podcast with id [" + podcastId + "] is null");
		return this.db
				.sql("select * from podcast_episode where podcast_id =? order by created ")
				.param(podcastId)
				.query(episodeRowMapper)
				.list();
	}

	@Override
	public Podcast createPodcast(Long mogulId, String title) {
		var kh = new GeneratedKeyHolder();
		this.db.sql(
				"insert into podcast (mogul_id, title) values (?,?) on conflict on constraint  podcast_mogul_id_title_key do update set title =excluded.title")
			.params(mogulId, title)
			.update(kh);
		var id = JdbcUtils.getIdFromKeyHolder(kh);
		return getPodcastById(id.longValue());
	}

	@Override
	public Episode createPodcastEpisode(Long podcastId, String title, String description, ManagedFile graphic,
			ManagedFile introduction, ManagedFile interview) {
		Assert.notNull(podcastId, "the podcast Id can not be null");
		Assert.hasText(title, "the title has text");
		Assert.hasText(description, "the description has text");
		Assert.notNull(graphic, "the graphic is not null ");
		Assert.notNull(introduction, "the introduction is not null ");
		Assert.notNull(interview, "the interview is not null ");
		var kh = new GeneratedKeyHolder();
		this.db.sql(
				"insert into podcast_episode(podcast_id, title, description,  graphic ,  introduction ,interview ) VALUES (?,?,?,?,?,? )")
			.params(podcastId, title, description, graphic.id(), introduction.id(), interview.id())
			.update(kh);
		var id = JdbcUtils.getIdFromKeyHolder(kh);
		return this.getEpisodeById(id.longValue());
	}

	@Override
	public Episode getEpisodeById(Long episodeId) {
		var res = db.sql("select * from podcast_episode where id =?").param(episodeId).query(episodeRowMapper).list();
		log.info("there are " + res.size() + " results.");
		return res.isEmpty() ? null : res.getFirst();
	}

	@Override
	public void deletePodcast(Long podcastId) {
		var eps = this.getEpisodesByPodcast(podcastId);
		Assert.state(eps.isEmpty(), "can't delete a podcast for which there are episodes");
		db.sql("delete from podcast where id= ?").param(podcastId).update();

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
		var image = managedFileService.createManagedFile(currentMogulId, bucket, uid, "image.jpg", 0);
		var intro = managedFileService.createManagedFile(currentMogulId, bucket, uid, "intro.mp3", 0);
		var interview = managedFileService.createManagedFile(currentMogulId, bucket, uid, "interview.mp3", 0);
		Assert.notNull(image, "the image managedFile is null");
		Assert.notNull(intro, "the intro managedFile is null");
		Assert.notNull(interview, "the interview managedFile is null");
		return createPodcastEpisode(podcastId, title, description, image, intro, interview);
	}

}

class PodcastRowMapper implements RowMapper<Podcast> {

	@Override
	public Podcast mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new Podcast(rs.getLong("mogul_id"), rs.getLong("id"), rs.getString("title"), rs.getDate("created"));
	}

}

class EpisodeRowMapper implements RowMapper<Episode> {

	private final Function<Long, Podcast> podcastFunction;

	private final Function<Long, ManagedFile> managedFileFunction;

	EpisodeRowMapper(Function<Long, Podcast> podcastFunction, Function<Long, ManagedFile> managedFileFunction) {
		this.podcastFunction = podcastFunction;
		this.managedFileFunction = managedFileFunction;
	}

	@Override
	public Episode mapRow(ResultSet rs, int rowNum) throws SQLException {

		var nullProducedAudio = rs.getLong("produced_audio");

		return new Episode(rs.getLong("id"), this.podcastFunction.apply(rs.getLong("podcast_id")),
				rs.getString("title"), rs.getString("description"), rs.getDate("created"),
				this.managedFileFunction.apply(rs.getLong("graphic")),
				this.managedFileFunction.apply(rs.getLong("introduction")),
				this.managedFileFunction.apply(rs.getLong("interview")),
				nullProducedAudio == 0 ? null : this.managedFileFunction.apply(rs.getLong("produced_audio")));
	}

}
