package com.joshlong.mogul.api;

import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
class DefaultMogulService implements MogulService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JdbcClient db;

	private final TransactionTemplate transactionTemplate;

	public DefaultMogulService(JdbcClient db, TransactionTemplate transactionTemplate) {
		this.db = db;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public Mogul getMogulById(Long id) {
		return this.db.sql("select * from mogul where id =? ")
			.param(id)
			.query(new MogulRowMapper(this::getPodbeanAccountByMogul))
			.single();
	}

	@Override
	public Mogul getMogulByName(String name) {
		return this.db.sql("select * from mogul where id =? ")
			.param(name)
			.query(new MogulRowMapper(this::getPodbeanAccountByMogul))
			.single();
	}

	@Override
	public PodbeanAccount getPodbeanAccountByMogul(Long mogul) {
		return this.db.sql("select * from podbean_account where mogul_id = ?")
			.param(mogul)
			.query(new PodbeanAccountRowMapper())
			.single();
	}

	@Override
	public PodbeanAccount configurePodbeanAccount(Long mogul, String clientId, String clientSecret) {
		var sql = """
				        insert into podbean_account (mogul_id,client_id, client_secret) values (?,?,?)
				        on conflict on constraint <BLAH> do update set
				        client_id = excluded.client_id,
				        client_secret = excluded.client_secret
				""";
		var updated = this.db.sql(sql).params(mogul, clientId, clientSecret).update();
		Assert.state(updated == 1 || updated == 0, "the update should result in an upsert");
		return getPodbeanAccountByMogul(mogul);
	}

	@Override
	public List<Podcast> getPodcastsByMogul(Long mogul) {
		return this.db.sql("select * from podcast where mogul_id = ? ")
			.param(mogul)
			.query(new PodcastRowMapper())
			.list();
	}

	@Override
	public List<Podcast> getAllPodcasts() {
		return this.db.sql("select * from podcast  ").query(new PodcastRowMapper()).list();
	}

	@Override
	public Podcast addPodcastEpisode(Long mogulId, Podcast podcast) {

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
				 uid ,
				 mogul_id
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
		return this.transactionTemplate.execute(status -> {

			var ctr = 1;
			var nnPodbean = nonNull((podcast).podbean());
			var nnbS3 = nonNull(podcast.s3());
			var nnS3Audio = nonNull(nnbS3.audio());
			var nnS3Photo = nonNull(nnbS3.photo());

			var al = new ArrayList<Map<String, Object>>();
			var kh = new GeneratedKeyHolder(al);
			var updated = db.sql(sql)
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
				.param(ctr++, mogulId)
				.update(kh, "id");

			log.info("wrote " + updated + " records");
			var podcastId = (Long) kh.getKey();
			return getPodcastById(podcastId);
		});
	}

	private String nullSafeUri(URI uri) {
		return uri == null ? null : uri.toString();
	}

	private final Podcast.S3 s3 = new Podcast.S3(new Podcast.S3.Audio(null, null), new Podcast.S3.Photo(null, null));

	private final Podcast.Podbean podbean = new Podcast.Podbean(null, null, null, null, null);

	private final Map<Class<?>, ?> defaults = Map.of(Podcast.S3.class, s3, Podcast.S3.Audio.class, s3.audio(),
			Podcast.S3.Photo.class, s3.photo(), Podcast.Podbean.class, podbean);

	@SafeVarargs
	@SuppressWarnings("unchecked")
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

	@Override
	public Podcast getPodcastByPodbeanEpisode(String id) {
		return this.transactionTemplate
			.execute(tx -> this.db.sql("select * from podcast where podbean_episode_id  =  ? ")
				.query(new PodcastRowMapper())
				.single());
	}

	@Override
	public Podcast markPodcastForPromotion(Podcast podcast) {
		var id = podcast.id();
		return this.transactionTemplate.execute(tx -> {
			this.db.sql("update podcast set needs_promotion = true where id =? ").param(id).update();
			return getPodcastById(id);
		});
	}

	private Podcast getPodcastById(Long id) {
		return this.db.sql("select * from podcast where id =? ").param(id).query(new PodcastRowMapper()).single();
	}

}

class PodbeanAccountRowMapper implements RowMapper<PodbeanAccount> {

	@Override
	public PodbeanAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new PodbeanAccount(rs.getLong("id"), rs.getString("client_id"), rs.getString("client_secret"));
	}

}

class PodcastRowMapper implements RowMapper<Podcast> {

	@Override
	public Podcast mapRow(ResultSet rs, int rowNum) throws SQLException {
		var id = rs.getLong("id");
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
		var podbean = new Podcast.Podbean(podbeanEpisodeId, podbeanDraftCreated, podbeanDraftPublished, podbeanPhotoUri,
				podbeanMediaUri);
		var mogulId = rs.getLong("mogul_id");
		return new Podcast(mogulId, id, uid, date, description, transcript, title, podbean, notes, s3);
	}

}

class MogulRowMapper implements RowMapper<Mogul> {

	private final Function<Long, PodbeanAccount> podbeanAccountFunction;

	MogulRowMapper(Function<Long, PodbeanAccount> podbeanAccountFunction) {
		this.podbeanAccountFunction = podbeanAccountFunction;
	}

	@Override
	public Mogul mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new Mogul(rs.getLong("id"), rs.getString("username"), rs.getString("email"), rs.getString("client_id"),
				podbeanAccountFunction.apply(rs.getLong("id")));
	}

}