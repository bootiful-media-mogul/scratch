package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulCreatedEvent;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.function.Function;

@Service
@Transactional
class DefaultPodcastService implements PodcastService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JdbcClient db;

	private final ApplicationEventPublisher publisher;

	private final ManagedFileService managedFileService;

	private final EpisodeRowMapper episodeRowMapper;

	// private boolean isValidMogul(Long mogulId) {
	// return mogulId != null && mogulService.getCurrentMogul().id().equals(mogulId);
	// }
	//
	// private void assertValidMogul(Long mogulId) {
	// Assert.state(isValidMogul(mogulId), "the mogul specified [" + mogulId +
	// "] is not currently authenticated");
	// }

	DefaultPodcastService(JdbcClient db, ManagedFileService managedFileService, ApplicationEventPublisher publisher) {
		this.db = db;
		this.managedFileService = managedFileService;
		this.publisher = publisher;
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
		return this.db.sql("select * from podcast where mogul_id =?")
			.param(mogulId)
			.query(new PodcastRowMapper())
			.list();
	}

	@Override
	public Collection<Episode> getEpisodesByPodcast(Long podcastId) {
		var podcast = getPodcastById(podcastId);
		Assert.notNull(podcast, "the podcast with id [" + podcastId + "] is null");
		return db.sql("select * from podcast_episode where podcast_id =? ")
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
		var id = (Number) kh.getKeys().get("id");
		return getPodcastById(id.longValue());
	}

	@Override
	public Episode createPodcastEpisode(Long podcastId, String title, String description, ManagedFile graphic,
			ManagedFile introduction, ManagedFile interview) {
		var kh = new GeneratedKeyHolder();
		this.db.sql(
				"insert into podcast_episode(podcast_id, title, description,  graphic ,  introduction ,interview ) VALUES (?,?,?,?,?,? )")
			.params(podcastId, title, description, graphic.toString(), introduction.toString(), interview.toString())
			.update();
		var id = (Number) kh.getKeys().get("id");
		return getEpisodeById(id.longValue());
	}

	@Override
	public Episode getEpisodeById(Long episodeId) {
		return db.sql("select * from podcast_episode where id =?").param(episodeId).query(episodeRowMapper).single();
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
		return new Episode(this.podcastFunction.apply(rs.getLong("podcast_id")), rs.getString("title"),
				rs.getString("description"), rs.getDate("created"),
				this.managedFileFunction.apply(rs.getLong("graphic")),
				this.managedFileFunction.apply(rs.getLong("introduction")),
				this.managedFileFunction.apply(rs.getLong("interview")),
				this.managedFileFunction.apply(rs.getLong("produced_audio")));
	}

}
