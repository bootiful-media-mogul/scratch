package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.podbean.Episode;
import com.joshlong.podbean.EpisodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
class PodbeanEpisodePublicationTracker {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TransactionTemplate transactionTemplate;

	private final JdbcClient jdbcClient;

	PodbeanEpisodePublicationTracker(TransactionTemplate transactionTemplate, JdbcClient jdbcClient) {
		this.transactionTemplate = transactionTemplate;
		this.jdbcClient = jdbcClient;
	}

	record NewlyPublishedEpisode(Long podcastId, Episode episode) {
	}

	Set<NewlyPublishedEpisode> identifyNewlyPublishedEpisodes(Collection<Episode> episodes) {
		try {
			return transactionTemplate.execute((tt) -> {
				var map = episodes.stream().collect(Collectors.toMap(Episode::getId, id -> id));
				return this.doUpdate(episodes) //
					.map(x -> new NewlyPublishedEpisode(x.podcastId(), map.get(x.podbeanEpisodeId())))
					.collect(Collectors.toSet());//
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private record UpdatedPodcast(String podbeanEpisodeId, Long podcastId) {
	}

	private Stream<UpdatedPodcast> doUpdate(Collection<Episode> episodes) {

		var revision = UUID.randomUUID().toString();

		jdbcClient.sql("update podbean_episode set previously_published = published ").update();

		for (var episode : episodes) {

			var sql = """
					    insert into podbean_episode (id, published , previously_published,title  , revision  )
					    values ( ?,?, ? ,?,?)
					    on conflict on constraint podbean_episode_id_key do update
					    set published = excluded.published  , revision = excluded.revision
					""";

			var published = StringUtils.hasText(episode.getStatus())
					&& episode.getStatus().equalsIgnoreCase(EpisodeStatus.PUBLISH.name());

			jdbcClient.sql(sql)
				.param(episode.getId())
				.param(published)
				.param(published)
				.param(episode.getTitle())
				.param(revision)
				.update();
		}

		record PublicationStatus(String podcastId, boolean published) {
		}

		var updatedEpisodeIds = this.jdbcClient.sql("""
				select id, published from podbean_episode where
				  published !=  previously_published
				and
				  revision = ?
				""")
			.param(revision)
			.query((rs, rowNum) -> new PublicationStatus(rs.getString("id"), rs.getBoolean("published")))
			.set();

		for (var podbeanPublicationStatus : updatedEpisodeIds) {
			var podbeanId = podbeanPublicationStatus.podcastId();

			var field = (podbeanPublicationStatus.published() ? " NOW() " : " NULL ");
			var ctr = this.jdbcClient.sql(
					"  update podcast set podbean_revision = ?, podbean_draft_published =  %s where podbean_episode_id = ? "
						.formatted(field))//
				.param(revision)
				.param(podbeanId)//
				.update();

			if (ctr > 0) {
				log.info("updated a row to reflect the fact that we've since changed publication status ["
						+ podbeanPublicationStatus.podcastId() + "] for an episode with ID [" + podbeanId + "]");
			}
		}

		return this.jdbcClient
			.sql("select * from podcast p where p.podbean_draft_published is not null and podbean_revision = ?")
			.param(revision)
			.query((rs, i) -> new UpdatedPodcast(rs.getString("podbean_episode_id"), rs.getLong("id")))
			.stream();

	}

	@Deprecated
	@SuppressWarnings("unused")
	// todo do something with this knowledge; maybe we could prompt the user to reconcile
	// these two things? force one to line up with the other?
	private Collection<Episode> nonReconciledEpisodes(Collection<Episode> episodes) {
		log.debug("got " + episodes.size() + " episodes to reconcile");
		return episodes.stream()
			// keep only the episodes for which we couldn't find a corresponding source
			// origin podcast record
			.filter(episode -> this.jdbcClient.sql("select id from podcast where title = ? ")
				.param(episode.getTitle())
				.query((rs, rowNum) -> rs.getLong("id"))
				.set()
				.isEmpty())
			.toList();
	}

}
