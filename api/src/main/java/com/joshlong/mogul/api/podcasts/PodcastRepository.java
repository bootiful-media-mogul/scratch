package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

@Repository
@Transactional
class PodcastRepository {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TransactionTemplate transactionTemplate;

	private final JdbcClient jdbcClient;



	PodcastRepository(TransactionTemplate transactionTemplate, JdbcClient jdbcClient ) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcClient = jdbcClient;
	}

	void persist(Podcast podcast) {

		var sql = """
				insert into podcast (
				 date,
				 description,
				 notes,
				 podbean_draft_created,
				 podbean_draft_published,
				 podbean_media_uri,
				 podbean_photo_uri,
				 s3_audio_file_name,
				 s3_audio_uri,
				 s3_photo_file_name,
				 s3_photo_uri,
				 title,
				 uid
				)
				values (
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?,
				 ?
					 )
					 on conflict (title)
					 do update
					 set
				            date = excluded.date,
				            description = excluded.description,
				            notes = excluded.notes,
				            podbean_draft_created = excluded.podbean_draft_created,
				            podbean_draft_published = excluded.podbean_draft_published,
				            podbean_media_uri = excluded.podbean_media_uri,
				            podbean_photo_uri = excluded.podbean_photo_uri,
				            s3_audio_file_name = excluded.s3_audio_file_name,
				            s3_audio_uri = excluded.s3_audio_uri,
				            s3_photo_file_name = excluded.s3_photo_file_name,
				            s3_photo_uri = excluded.s3_photo_uri,
				            title = excluded.title,
				            uid = excluded.uid
				""";
		this.transactionTemplate.execute(status -> {

			var ctr = 1;
			var nnPodbean = nonNull( (podcast).podbean());
			var nnbS3 = nonNull(podcast.s3());
			var nnS3Audio = nonNull(nnbS3.audio());
			var nnS3Photo = nonNull(nnbS3.photo());

			var updated = jdbcClient.sql(sql)
				.param(ctr++, podcast.date())
				.param(ctr++, podcast.description())
				.param(ctr++, podcast.notes())
				.param(ctr++, nnPodbean.draftCreated())
				.param(ctr++, nnPodbean.draftPublished())
				.param(ctr++, nullSafeUri(nnPodbean.media()))
				.param(ctr++, nullSafeUri(nnPodbean.photo()))
				.param(ctr++, nnS3Audio.fileName())
				.param(ctr++, nullSafeUri(nnS3Audio.uri()))
				.param(ctr++, nnS3Photo.fileName())
				.param(ctr++, nullSafeUri(nnS3Photo.uri()))
				.param(ctr++, podcast.title())
				.param(ctr++, podcast.uid())
				.update();

			log.info("wrote " + updated + " records");
			return null;
		});
	}

	private String nullSafeUri(URI uri) {
		return uri == null ? null : uri.toString();
	}

	private final Podcast.S3 s3 = new Podcast.S3(new Podcast.S3.Audio(null, null), new Podcast.S3.Photo(null, null));

	private final Podcast.Podbean podbean = new Podcast.Podbean(null, null, null, null, null);

	private final Map<Class<?>, ?> defaults = Map.of(Podcast.S3.class, s3, Podcast.S3.Audio.class, s3.audio(),
			Podcast.S3.Photo.class, s3.photo(), Podcast.Podbean.class, podbean);

	private <T> T nonNull(T input, T... args) {
		if (null == input) {
			var typeOfVar = args.getClass().getComponentType();
			var rt = (T) defaults.get(typeOfVar);
			if (log.isDebugEnabled())
				log.debug("returning " + rt + " for query class [" + typeOfVar.getName() + "]");
			return rt;
		}
		return input;
	}

	Collection<Podcast> podcasts() {
		return this.transactionTemplate.execute((tx) -> this.jdbcClient.sql(" select * from podcast order by date asc ")
			.query(new PodcastRowMapper())
			.list());
	}

	Podcast podcastById(Integer id) {
		return this.transactionTemplate.execute((tx) -> this.jdbcClient.sql("select * from podcast where id = ? ")
			.param(id)
			.query(new PodcastRowMapper())
			.single());
	}

	Collection<Podcast> podcastByPodbeanEpisodeId(String id) {
		return this.transactionTemplate
			.execute(tx -> this.jdbcClient.sql("select * from podcast where podbean_episode_id  =  ? ")
				.query(new PodcastRowMapper())
				.list());
	}

	void markAsNeedingPromotion(Podcast podcast) {
		this.transactionTemplate.execute(tx -> {
			this.jdbcClient.sql("update podcast set needs_promotion = true where id =? ").param(podcast.id()).update();
			return null;
		});
	}

	private static class PodcastRowMapper implements RowMapper<Podcast> {

		@Override
		public Podcast mapRow(ResultSet rs, int rowNum) throws SQLException {
			var id = rs.getInt("id");
			var uid = rs.getString("uid");
			var date = rs.getDate("date");
			var description = rs.getString("description");
			var notes = rs.getString("notes");
			var podbeanDraftCreated = rs.getDate("podbean_draft_created");
			var podbeanDraftPublished = rs.getDate("podbean_draft_published");
			var podbeanMediaUri = JdbcUtils.uri(rs, "podbean_media_uri");
			var podbeanPhotoUri = JdbcUtils.uri(rs, "podbean_photo_uri");
			var s3AudioFileName = rs.getString("s3_audio_file_name");
			var s3AudioUri = JdbcUtils.uri(rs, "s3_audio_uri");
			var s3PhotoFileName = rs.getString("s3_photo_file_name");
			var s3PhotoUri = JdbcUtils.uri(rs, "s3_photo_uri");
			var transcript = rs.getString("transcript");
			var title = rs.getString("title");
			var podbeanEpisodeId = rs.getString("podbean_episode_id");
			var s3 = new Podcast.S3(new Podcast.S3.Audio(s3AudioUri, s3AudioFileName),
					new Podcast.S3.Photo(s3PhotoUri, s3PhotoFileName));
			var podbean = new Podcast.Podbean(podbeanEpisodeId, podbeanDraftCreated, podbeanDraftPublished,
					podbeanPhotoUri, podbeanMediaUri);
			return new Podcast(id, uid, date, description, transcript, title, podbean, notes, s3);
		}

	}

}
