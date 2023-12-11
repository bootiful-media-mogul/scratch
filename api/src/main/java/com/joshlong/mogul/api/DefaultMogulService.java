package com.joshlong.mogul.api;

import com.joshlong.mogul.api.old.podbean.PodbeanPublication;
import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
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
import java.util.List;
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

	private final ApplicationEventPublisher publisher;

	private final Settings settings;

	DefaultMogulService(JdbcClient jdbcClient, TransactionTemplate transactionTemplate,
			ApplicationEventPublisher publisher, Settings settings) {
		this.db = jdbcClient;
		this.transactionTemplate = transactionTemplate;
		this.publisher = publisher;
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
		var principalName = principal.getName();
		var exists = getMogulByName(principalName) != null;
		var sql = """
				insert into mogul(username,  client_id) values (?, ?)
				on conflict on constraint mogul_client_id_username_key do nothing
				""";
		if (principal.getPrincipal() instanceof Jwt jwt && jwt.getClaims().get("aud") instanceof List list
				&& list.get(0) instanceof String aud) {
			this.db.sql(sql).params(principalName, aud).update();
		}
		var mogul = this.getMogulByName(principalName);
		if (!exists) {
			publisher.publishEvent(new MogulCreatedEvent(mogul));
		}
		return mogul;

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
		var moguls = this.db//
			.sql("select * from mogul where  username  = ? ")
			.param(name)
			.query(new MogulRowMapper(this::getPodbeanAccountSettings))
			.list();
		Assert.state(moguls.size() <= 1, "there should only be one mogul with this username [" + name + "]");
		return moguls.isEmpty() ? null : moguls.getFirst();
	}

	@Override
	public void assertAuthorizedMogul(Long aLong) {

		var currentlyAuthenticated = getCurrentMogul();
		Assert.state(currentlyAuthenticated != null && currentlyAuthenticated.id().equals(aLong),
				"the requested mogul [" + aLong + "] is not currently authenticated");

	}
	/*
	 *
	 * public PodbeanAccountSettings configurePodbeanAccountSettings(Long mogulId, String
	 * clientId, String clientSecret) { this.settings.set(mogulId,
	 * PODBEAN_ACCOUNTS_SETTINGS, PODBEAN_ACCOUNTS_SETTINGS_CLIENT_SECRET, clientSecret);
	 * this.settings.set(mogulId, PODBEAN_ACCOUNTS_SETTINGS,
	 * PODBEAN_ACCOUNTS_SETTINGS_CLIENT_ID, clientId); return
	 * getPodbeanAccountSettings(mogulId); }
	 */

	public PodbeanAccountSettings getPodbeanAccountSettings(Long mogulId) {
		var clientId = this.settings.getString(mogulId, PODBEAN_ACCOUNTS_SETTINGS, PODBEAN_ACCOUNTS_SETTINGS_CLIENT_ID);
		var clientSecret = this.settings.getString(mogulId, PODBEAN_ACCOUNTS_SETTINGS,
				PODBEAN_ACCOUNTS_SETTINGS_CLIENT_SECRET);
		var configured = StringUtils.hasText(clientId) || StringUtils.hasText(clientSecret);
		return configured ? new PodbeanAccountSettings(clientId, clientSecret) : null;
	}

	/*
	 * public List<Podcast> getPodcastsByMogul(Long mogul) { return this.db// .sql("""
	 * select * from podcast where mogul_id = ? and deleted = false and id in ( select
	 * pd.podcast_id from podcast_draft pd where pd.podcast_id=id and pd.completed = true
	 * ) """) .param(mogul) .query(new PodcastRowMapper()) .list(); }
	 *
	 *
	 * public List<Podcast> getAllPodcasts() { return
	 * this.db.sql("select * from podcast  ").query(new PodcastRowMapper()).list(); }
	 *
	 *
	 * public PodcastDraft updatePodcastDraft(Long mogulId, String uid, String title,
	 * String description, Resource pictureFN, Resource introFN, Resource interviewFN,
	 * boolean completed) {
	 *
	 * Assert.hasText(uid, "the uid must be non-null"); Assert.notNull(mogulId,
	 * "the mogulId must be non-null"); Assert.hasText(title,
	 * "the title must be non-null"); Assert.hasText(description,
	 * "the description must be non-null"); Assert.notNull(pictureFN,
	 * "the picture file name must be non-null"); Assert.notNull(introFN,
	 * "the introduction file name must be non-null"); Assert.notNull(interviewFN,
	 * "the interview file name must be non-null");
	 *
	 * var sql = """ insert into podcast_draft (uid, title, description, mogul_id ,
	 * picture_file_name, intro_file_name, interview_file_name ) values (?,?,?,?,?,?,? )
	 * on conflict on constraint podcast_draft_uid_key do update set title =
	 * excluded.title, description = excluded.description, picture_file_name =
	 * excluded.picture_file_name, intro_file_name = excluded.intro_file_name,
	 * interview_file_name = excluded.interview_file_name """; this.db.sql(sql)
	 * .params(uid, title, description, completed, mogulId, pictureFN.getFilename(),
	 * introFN.getFilename(), interviewFN.getFilename()) .update();
	 * markPodcastDraftCompleted(uid, completed); return getPodcastDraftByUid(uid); }
	 *
	 * private void markPodcastDraftCompleted(String podcastDraftUid, boolean completed) {
	 * var draft = getPodcastDraftByUid(podcastDraftUid);
	 * this.db.sql("update podcast_draft set completed = ? where uid=?").params(completed,
	 * draft.uid()).update(); }
	 *
	 *
	 * public Podcast addPodcastEpisode(Long mogulId, Podcast podcast) {
	 *
	 * var sql = """ insert into podcast ( date, description, notes, podbean_media_uri,
	 * podbean_photo_uri, s3_audio_file_name, s3_audio_uri, s3_photo_file_name,
	 * s3_photo_uri, title, uid , mogul_id ) values ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )
	 * on conflict on constraint podcast_mogul_id_title_key do update set date =
	 * excluded.date, description = excluded.description, notes = excluded.notes,
	 * podbean_media_uri = excluded.podbean_media_uri, podbean_photo_uri =
	 * excluded.podbean_photo_uri, s3_audio_file_name = excluded.s3_audio_file_name,
	 * s3_audio_uri = excluded.s3_audio_uri, s3_photo_file_name =
	 * excluded.s3_photo_file_name, s3_photo_uri = excluded.s3_photo_uri, title =
	 * excluded.title, uid = excluded.uid """; return
	 * this.transactionTemplate.execute(status -> {
	 *
	 * var ctr = 1; var nnPodbean = nonNull((podcast).podbean()); var nnbS3 =
	 * nonNull(podcast.s3()); var nnS3Audio = nonNull(nnbS3.audio()); var nnS3Photo =
	 * nonNull(nnbS3.photo());
	 *
	 * var al = new ArrayList<Map<String, Object>>(); var kh = new GeneratedKeyHolder(al);
	 * var updated = db.sql(sql) .param(ctr++, podcast.date()) .param(ctr++,
	 * podcast.description()) .param(ctr++, podcast.notes()) .param(ctr++,
	 * nullSafeUri(nnPodbean.media())) .param(ctr++, nullSafeUri(nnPodbean.photo()))
	 * .param(ctr++, nnS3Audio.fileName()) .param(ctr++, nullSafeUri(nnS3Audio.uri()))
	 * .param(ctr++, nnS3Photo.fileName()) .param(ctr++, nullSafeUri(nnS3Photo.uri()))
	 * .param(ctr++, podcast.title()) .param(ctr++, podcast.uid()) .param(ctr++, mogulId)
	 * .update(kh, "id");
	 *
	 * log.info("wrote " + updated + " records"); var podcastId =
	 * Objects.requireNonNull(kh.getKey()).longValue();
	 *
	 * // todo connect podcast to podcast_draft via the UID
	 *
	 * db.sql("update podcast_draft set podcast_id = ? where uid =?").params(podcastId,
	 * podcast.uid()).update();
	 *
	 * return getPodcastById(podcastId); }); }
	 */
	private String nullSafeUri(URI uri) {
		return uri == null ? null : uri.toString();
	}
	/*
	 * private final Podcast.S3 s3 = new Podcast.S3(new Podcast.S3.Audio(null, null), new
	 * Podcast.S3.Photo(null, null));
	 *
	 * private final Podcast.Podbean podbean = new Podcast.Podbean(null, null, null, null,
	 * null);
	 *
	 * private final Map<Class<?>, ?> defaults = Map.of(Podcast.S3.class, s3,
	 * Podcast.S3.Audio.class, s3.audio(), Podcast.S3.Photo.class, s3.photo(),
	 * Podcast.Podbean.class, podbean);
	 *
	 * @SafeVarargs
	 *
	 * @SuppressWarnings("unchecked") private <T> T nonNull(T input, T... args) { if (null
	 * == input) { var typeOfVar = args.getClass().getComponentType(); var rt = (T)
	 * defaults.get(typeOfVar); log.debug("returning " + rt + " for query class [" +
	 * typeOfVar.getName() + "]"); return rt; } return input; }
	 */
	/*
	 *
	 * // @Override public Podcast connectPodcastToPodbeanPublication(Podcast podcast,
	 * String podbeanEpisodeId, URI podbeanMediaUrl, URI logoUrl, URI podbeanPlayerUrl) {
	 * return this.transactionTemplate.execute(tx -> {
	 *
	 * this.db.sql(""" update podcast set podbean_episode_id =? , podbean_media_uri = ? ,
	 * podbean_photo_uri = ? , podbean_player_uri = ? where id =? """)
	 * .params(podbeanEpisodeId, podbeanMediaUrl.toString(), logoUrl.toString(),
	 * podbeanPlayerUrl.toString(), podcast.id()) .update();
	 *
	 * // todo markPodcastDraftCompleted(podcast.uid(), true);
	 *
	 * return getPodcastById(podcast.id()); }); }
	 *
	 * // @Override public Podcast confirmPodbeanPublication(Podcast podcast, URI
	 * permalinkUrl, int duration) {
	 *
	 * return this.transactionTemplate.execute(tx -> { this.db.sql(""" update podcast set
	 * podbean_permalink_uri = ? , duration = ? , needs_promotion = true where id =?
	 * """)/// .params(permalinkUrl == null ? null : permalinkUrl.toString(), duration,
	 * podcast.id()) .update(); this.db.sql(
	 * "update podbean_publication_tracker set continue_tracking = false  , stopped = ? where podcast_id = ? "
	 * ) .params(new Date(), podcast.id()) .update();
	 * log.debug("confirmed publication for podcast [" + podcast + "]");
	 *
	 * markPodcastDraftCompleted(podcast.uid(), true);
	 *
	 * return getPodcastById(podcast.id()); }); } // // @Override public
	 * PodbeanPublication getPodbeanPublicationByPodcast(Podcast podcast) {
	 *
	 * var sql = """ select * from podbean_publication_tracker where mogul_id = ? and
	 * podcast_id = ?
	 *
	 * """; var all = db.sql(sql) .params(podcast.mogulId(), podcast.id()) .query(new
	 * PodbeanPublicationTrackerRowMapper()) .list(); if (all.isEmpty()) return null;
	 * return all.getFirst();
	 *
	 * }
	 *
	 * // @Override public Collection<PodbeanPublication>
	 * getOutstandingPodbeanPublications() { var sql = """ select * from
	 * podbean_publication_tracker where node_id = ? and stopped is null """; return
	 * db.sql(sql).params(NodeUtils.nodeId()).query(new
	 * PodbeanPublicationTrackerRowMapper()).list(); }
	 *
	 * // @Override public Collection<PodcastDraft> getPodcastDraftsByMogul(Long mogulId)
	 * { return this.db//
	 * .sql("select * from podcast_draft where completed = false and mogul_id = ?")//
	 * .param(mogulId)// .query(new PodcastDraftRowMapper())// .list(); }
	 *
	 * // @Override public Collection<Podcast> getDeletedPodcasts() { return
	 * this.db.sql("select * from podcast where deleted = true ").query(new
	 * PodcastRowMapper()).list(); }
	 *
	 * // @Override public PodbeanPublication monitorPodbeanPublication(String nodeName,
	 * Podcast podcast) { var sql = """ insert into podbean_publication_tracker ( node_id,
	 * mogul_id, continue_tracking, podcast_id, started, stopped ) values (?, ?, ?, ?, ?,
	 * ? ) on conflict on constraint podbean_publication_tracker_podcast_id_key do nothing
	 * """;
	 *
	 * this.db.sql(sql).params(nodeName, podcast.mogulId(), true, podcast.id(), new
	 * Date(), null).update();
	 *
	 * return getPodbeanPublicationByPodcast(podcast);
	 *
	 * }
	 *
	 * // @Override public PodcastDraft createPodcastDraft(Long mogulId, String uuid) {
	 * var sql = """ insert into podcast_draft (uid, date, title, description, completed ,
	 * mogul_id ) values (?, ? , ? , ?, ? ,? ) on conflict on constraint
	 * podcast_draft_uid_key do update set title = excluded.title, description =
	 * excluded.description, date = excluded.date, completed = excluded.completed """;
	 * this.db.sql(sql).params(uuid, new Date(), null, null, false, mogulId).update();
	 *
	 * return getPodcastDraftByUid(uuid); }
	 *
	 * // @Override public boolean deletePodcast(Long podcastId) { var podcast =
	 * getPodcastById(podcastId); Assert.notNull(podcast, "the podcast with id [" +
	 * podcastId + "] does not exist!");
	 * this.db.sql("delete from podbean_publication_tracker where podcast_id = ?").param(
	 * podcastId).update();
	 * this.db.sql("delete from podcast_draft where podcast_id = ?").param(podcastId).
	 * update();
	 * this.db.sql("delete from podcast where id = ?").param(podcastId).update(); return
	 * true; }
	 *
	 * // @Override public boolean schedulePodcastForDeletion(Long podcastId) { var
	 * podcast = getPodcastById(podcastId); Assert.notNull(podcast,
	 * "the podcast with id [" + podcastId + "] does not exist!");
	 * this.db.sql(" update podcast p set deleted = true where id = ? ").param(podcastId).
	 * update();
	 *
	 * return true; }
	 *
	 * // @Override public PodcastDraft getPodcastDraftByUid(String uuid) { return
	 * this.db.sql("select * from podcast_draft where uid =? ") .param(uuid) .query(new
	 * PodcastDraftRowMapper()) .single(); }
	 *
	 * // @Override public Podcast getPodcastById(Long id) { return
	 * this.db.sql("select * from podcast where id =? ").param(id).query(new
	 * PodcastRowMapper()).single(); }
	 */

}
/*
 *
 * class PodcastRowMapper implements RowMapper<Podcast> {
 *
 * @Override public Podcast mapRow(ResultSet rs, int rowNum) throws SQLException { var id
 * = rs.getLong("id"); var uid = rs.getString("uid"); var date = rs.getDate("date"); var
 * description = rs.getString("description"); var notes = rs.getString("notes"); var
 * podbeanMediaUri = JdbcUtils.uri(rs, "podbean_media_uri"); var podbeanPhotoUri =
 * JdbcUtils.uri(rs, "podbean_photo_uri"); var playerUri = JdbcUtils.uri(rs,
 * "podbean_player_uri"); var permalinkUri = JdbcUtils.uri(rs, "podbean_permalink_uri");
 * var s3AudioFileName = rs.getString("s3_audio_file_name"); var s3AudioUri =
 * JdbcUtils.uri(rs, "s3_audio_uri"); var s3PhotoFileName =
 * rs.getString("s3_photo_file_name"); var s3PhotoUri = JdbcUtils.uri(rs, "s3_photo_uri");
 * var transcript = rs.getString("transcript"); var title = rs.getString("title"); var
 * podbeanEpisodeId = rs.getString("podbean_episode_id"); var s3 = new Podcast.S3(new
 * Podcast.S3.Audio(s3AudioUri, s3AudioFileName), new Podcast.S3.Photo(s3PhotoUri,
 * s3PhotoFileName)); var podbean = new Podcast.Podbean(podbeanEpisodeId, podbeanPhotoUri,
 * podbeanMediaUri, playerUri, permalinkUri); var mogulId = rs.getLong("mogul_id"); return
 * new Podcast(mogulId, id, uid, date, description, transcript, title, podbean, notes,
 * s3); }
 *
 * }
 */

/*
 * class PodbeanPublicationTrackerRowMapper implements RowMapper<PodbeanPublication> {
 *
 * @Override public PodbeanPublication mapRow(ResultSet rs, int rowNum) throws
 * SQLException { return new PodbeanPublication(rs.getLong("mogul_id"),
 * rs.getLong("podcast_id"), rs.getString("node_id"), rs.getBoolean("continue_tracking"),
 * rs.getDate("started"), rs.getDate("stopped")); }
 *
 * }
 */

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
/*
 *
 * class PodcastDraftRowMapper implements RowMapper<PodcastDraft> {
 *
 * @Override public PodcastDraft mapRow(ResultSet rs, int rowNum) throws SQLException {
 * return new PodcastDraft(rs.getLong("id"), rs.getBoolean("completed"),
 * rs.getString("uid"), rs.getDate("date"), rs.getString("title"),
 * rs.getString("description"), rs.getString("intro_file_name"),
 * rs.getString("interview_file_name"), rs.getString("picture_file_name"),
 * rs.getLong("mogul_id")); }
 *
 * }
 */
