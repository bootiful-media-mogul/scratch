package com.joshlong.mogul.api;

import com.joshlong.mogul.api.podcasts.podbean.PodbeanPublication;
import com.joshlong.mogul.api.utils.JdbcUtils;
import com.joshlong.mogul.api.utils.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

@Service
@Transactional
class DefaultMogulService implements MogulService {

	//
	private final static String PODBEAN_ACCOUNTS_SETTINGS = "podbean";

	private final static String PODBEAN_ACCOUNTS_SETTINGS_CLIENT_ID = "client-id";

	private final static String PODBEAN_ACCOUNTS_SETTINGS_CLIENT_SECRET = "client-secret";

	//
	private final Logger log = LoggerFactory.getLogger(getClass());

	//
	private final JdbcClient db;

	private final TransactionTemplate transactionTemplate;

	private final Settings settings;

	DefaultMogulService(JdbcClient jdbcClient, TransactionTemplate transactionTemplate, Settings settings) {
		this.db = jdbcClient;
		this.transactionTemplate = transactionTemplate;
		this.settings = settings;
		Assert.notNull(this.settings, "the settings are null");
		Assert.notNull(this.db, "the db is null");
		Assert.notNull(this.transactionTemplate, "the transactionTemplate is null");
	}

	@Override
	public Mogul getCurrentMogul() {
		var name = SecurityContextHolder.getContextHolderStrategy().getContext().getAuthentication().getName();
		return this.getMogulByName(name);
	}

	@Override
	public Mogul login(Authentication principal) {
		var sql = """
				insert into mogul(username,  client_id) values (?, ?)
				on conflict on constraint mogul_client_id_username_key do nothing
				""";
		if (principal.getPrincipal() instanceof Jwt jwt && jwt.getClaims().get("aud") instanceof List list
				&& list.get(0) instanceof String aud) {
			var name = principal.getName();
			this.db.sql(sql).params(name, aud).update();
			return this.getMogulByName(name);
		}
		throw new IllegalStateException(
				"failed to register a new user with authentication [" + principal.toString() + "]");
	}

	@Override
	public Mogul getMogulById(Long id) {
		return this.db.sql("select * from mogul where id =? ")
			.param(id)
			.query(new MogulRowMapper(this::getPodbeanAccountSettings))
			.single();
	}

	@Override
	public Mogul getMogulByName(String name) {
		return this.db.sql("select * from mogul where  username  = ? ")
			.param(name)
			.query(new MogulRowMapper(this::getPodbeanAccountSettings))
			.single();
	}

	@Override
	public PodbeanAccountSettings configurePodbeanAccountSettings(Long mogulId, String clientId, String clientSecret) {
		this.settings.set(mogulId, PODBEAN_ACCOUNTS_SETTINGS, PODBEAN_ACCOUNTS_SETTINGS_CLIENT_SECRET, clientSecret);
		this.settings.set(mogulId, PODBEAN_ACCOUNTS_SETTINGS, PODBEAN_ACCOUNTS_SETTINGS_CLIENT_ID, clientId);
		return getPodbeanAccountSettings(mogulId);
	}

	@Override
	public PodbeanAccountSettings getPodbeanAccountSettings(Long mogulId) {
		var clientId = this.settings.getString(mogulId, PODBEAN_ACCOUNTS_SETTINGS, PODBEAN_ACCOUNTS_SETTINGS_CLIENT_ID);
		var clientSecret = this.settings.getString(mogulId, PODBEAN_ACCOUNTS_SETTINGS,
				PODBEAN_ACCOUNTS_SETTINGS_CLIENT_SECRET);
		var configured = StringUtils.hasText(clientId) || StringUtils.hasText(clientSecret);
		return configured ? new PodbeanAccountSettings(clientId, clientSecret) : null;
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
	public PodcastDraft completePodcastDraft(Long mogulId, String uid, String title, String description,
			Resource pictureFN, Resource introFN, Resource interviewFN) {

		Assert.hasText(uid, "the uid must be non-null");
		Assert.notNull(mogulId, "the mogulId must be non-null");
		Assert.hasText(title, "the title must be non-null");
		Assert.hasText(description, "the description must be non-null");
		Assert.notNull(pictureFN, "the picture file name must be non-null");
		Assert.notNull(introFN, "the introduction file name must be non-null");
		Assert.notNull(interviewFN, "the interview file name must be non-null");

		var sql = """

				insert into podcast_draft (uid,  title, description, completed , mogul_id , picture_file_name, intro_file_name, interview_file_name )
				values (?,?,?,?,?,?,?,?)
				on conflict on constraint podcast_draft_uid_key do update set
				title = excluded.title,
				description = excluded.description,
				completed = excluded.completed ,
				picture_file_name = excluded.picture_file_name,
				intro_file_name = excluded.intro_file_name,
				interview_file_name = excluded.interview_file_name
				""";
		this.db.sql(sql)
			.params(uid, title, description, true, mogulId, pictureFN.getFilename(), introFN.getFilename(),
					interviewFN.getFilename())
			.update();
		return getPodcastDraftByUid(uid);
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
				on conflict on constraint podcast_mogul_id_title_key
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
			var podcastId = Objects.requireNonNull(kh.getKey()).longValue();
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
	public Podcast confirmPodbeanPublication(Podcast podcast, String podbeanEpisodeId) {

		return this.transactionTemplate.execute(tx -> {
			this.db.sql("update podcast set podbean_episode_id =? ,  needs_promotion = true where id =? ")
				.params(podbeanEpisodeId, podcast.id())
				.update();
			this.db.sql(
					"update podbean_publication_tracker set continue_tracking = false  , stopped = ? where podcast_id = ? ")
				.params(new Date(), podcast.id())
				.update();
			return getPodcastById(podcast.id());
		});
	}

	@Override
	public PodbeanPublication getPodbeanPublicationByPodcast(Podcast podcast) {

		var sql = """
					select * from podbean_publication_tracker where
					mogul_id = ?
					and
					podcast_id = ?
					and
					stopped is null
				""";
		var all = db.sql(sql)
			.params(podcast.mogulId(), podcast.id())
			.query(new PodbeanPublicationTrackerRowMapper())
			.list();
		if (all.isEmpty())
			return null;
		return all.getFirst();

	}

	@Override
	public Collection<PodbeanPublication> getOutstandingPodbeanPublications() {
		var sql = """
				select * from podbean_publication_tracker where node_id = ? and stopped  is   null
				""";
		return db.sql(sql).params(NodeUtils.nodeId()).query(new PodbeanPublicationTrackerRowMapper()).list();
	}

	@Override
	public Collection<PodbeanPublication> getPodbeanPublicationsByNode(String nodeName) {
		var mogulPodcasts = """
				select  * from podbean_publication_tracker where node_id = ?
				""";
		return db.sql(mogulPodcasts).param(nodeName).query(new PodbeanPublicationTrackerRowMapper()).list();
	}

	@Override
	public PodbeanPublication monitorPodbeanPublication(String nodeName, Podcast podcast) {
		var sql = """
				insert into podbean_publication_tracker (  node_id, mogul_id, continue_tracking, podcast_id, started, stopped )
				values (?, ?, ?, ?, ?, ? )
				on conflict on constraint  podbean_publication_tracker_podcast_id_key
				do nothing
				""";

		this.db.sql(sql).params(nodeName, podcast.mogulId(), true, podcast.id(), new Date(), null).update();

		return getPodbeanPublicationByPodcast(podcast);

	}

	@Override
	public PodcastDraft createPodcastDraft(Long mogulId, String uuid) {
		var sql = """
				insert into podcast_draft (uid, date, title, description, completed , mogul_id ) values (?, ? , ? , ?, ? ,? )
				on conflict on constraint podcast_draft_uid_key do update set
				 title = excluded.title,
				 description = excluded.description,
				 date = excluded.date,
				 completed = excluded.completed
				""";
		this.db.sql(sql).params(uuid, new Date(), null, null, false, mogulId).update();

		return getPodcastDraftByUid(uuid);
	}

	@Override
	public PodcastDraft getPodcastDraftByUid(String uuid) {
		return this.db.sql("select * from podcast_draft where uid =? ")
			.param(uuid)
			.query(new PodcastDraftRowMapper())
			.single();
	}

	@Override
	public Podcast getPodcastById(Long id) {
		return this.db.sql("select * from podcast where id =? ").param(id).query(new PodcastRowMapper()).single();
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

class PodbeanPublicationTrackerRowMapper implements RowMapper<PodbeanPublication> {

	@Override
	public PodbeanPublication mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new PodbeanPublication(rs.getLong("mogul_id"), rs.getLong("podcast_id"), rs.getString("node_id"),
				rs.getBoolean("continue_tracking"), rs.getDate("started"), rs.getDate("stopped"));
	}

}

class MogulRowMapper implements RowMapper<Mogul> {

	private final Function<Long, PodbeanAccountSettings> podbeanAccountFunction;

	MogulRowMapper(Function<Long, PodbeanAccountSettings> podbeanAccountFunction) {
		this.podbeanAccountFunction = podbeanAccountFunction;
	}

	@Override
	public Mogul mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new Mogul(rs.getLong("id"), rs.getString("username"), rs.getString("email"), rs.getString("client_id"),
				podbeanAccountFunction.apply(rs.getLong("id")));
	}

}

class PodcastDraftRowMapper implements RowMapper<PodcastDraft> {

	@Override
	public PodcastDraft mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new PodcastDraft(rs.getLong("id"), rs.getBoolean("completed"), rs.getString("uid"), rs.getDate("date"),
				rs.getString("title"), rs.getString("description"), rs.getString("intro_file_name"),
				rs.getString("interview_file_name"), rs.getString("picture_file_name"), rs.getLong("mogul_id"));
	}

}