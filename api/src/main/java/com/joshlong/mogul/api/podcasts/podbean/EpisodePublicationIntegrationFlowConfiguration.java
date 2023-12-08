package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.podbean.Episode;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.awt.desktop.AppReopenedEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class EpisodePublicationIntegrationFlowConfiguration {

	private final Logger log = LoggerFactory.getLogger(getClass());


	@Bean
	ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler() {
		var aepm = new ApplicationEventPublishingMessageHandler();
		aepm.setPublishPayload(true);
		return aepm;
	}

	@EventListener
	void handlePodbeanEpisodePublishedEvent(PodbeanEpisodePublishedEvent event) {
		log.info("watch out world! we've just published a new episode! [" + event + "]");
	}


	// todo
	// todo now that the mogul has an outstanding unpublished podcast, add them to a table
	// of podbean-watchers or something
	// todo have some integration flow that runs every minute (or whatever) pull those
	// values down and then launch IntegrationFlows to monitor for their episodes
	// todo each launched IntegrationFlow should be in a
	// ConcurrentHashMap<Long,IntegrationFlow>
	// todo there should also be a boolean that each IntegrationFlow should consult when
	// deciding whether to poison-pill or continue: ConcurrentHashMap<Long,AtomicBoolean>
	// todo 3 things will set the boolean to false: a) the IntegrationFlow has been
	// running too long b) the episode was eventually published and we reconciled c) the
	// user pushed the 'i published it!' button
	// todo maybe instead of maps, we do all the tracking in a SQL table? and while we're
	// at it, we can note the node on which a watcher is executing?
	//
	// PODBEAN_TRACKERS
	// node_id : string, mogul_id: number, continue_watching: boolean, podcast_id: number,
	// started :timestamp , finished: timestamp?

	/*
	 * Run the following queries to see this flow in action: UPDATE PODCAST SET
	 * PODBEAN_DRAFT_CREATED = NULL, REVISION = NULL ; UPDATE PODBEAN_EPISODE SET
	 * PREVIOUSLY_PUBLISHED = FALSE , PUBLISHED = FALSE ;
	 */
	IntegrationFlow episodePublicationIntegrationFlow(PodbeanEpisodePublicationTracker podbeanEpisodePublicationTracker,
			ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler,
			PodbeanClient podbeanClient) {
		var messageSource = (MessageSource<Collection<Episode>>) () -> MessageBuilder
			.withPayload(podbeanClient.getAllEpisodes())
			.build();
		return IntegrationFlow
			.from(messageSource,
					pm -> pm.poller(p -> PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofSeconds(0))))
			.transform(
					(GenericTransformer<Collection<Episode>, Collection<PodbeanEpisodePublicationTracker.NewlyPublishedEpisode>>) podbeanEpisodePublicationTracker::identifyNewlyPublishedEpisodes)
			.filter((GenericSelector<Collection<Episode>>) source -> !source.isEmpty())
			.split(new AbstractMessageSplitter() {
				@Override
				protected Object splitMessage(Message<?> message) {
					return message.getPayload();
				}
			})
			.transform(
					(GenericTransformer<PodbeanEpisodePublicationTracker.NewlyPublishedEpisode, PodbeanEpisodePublishedEvent>) source -> new PodbeanEpisodePublishedEvent(
							source.podcastId(), source.episode()))
			.handle(episodeSyncApplicationEventPublishingMessageHandler)
			.get();
	}

}

@Component
class Runner {

	private final PodbeanPublicationWatcher podbeanPublicationWatcher;

	Runner(PodbeanPublicationWatcher podbeanPublicationWatcher) {
		this.podbeanPublicationWatcher = podbeanPublicationWatcher;
	}

	@EventListener(ApplicationReadyEvent.class)
	void run() throws Exception {
		var podcastId = 1;
		var mogulId = 1;
		this.podbeanPublicationWatcher.watch(mogulId, podcastId);
	}
}

@Configuration
class PodbeanPublicationWatcherConfiguration {

	@Bean
	PodbeanPublicationWatcher podbeanPublicationWatcher(
			JdbcClient db, ApplicationEventPublishingMessageHandler handler,
			PodbeanClient podbeanClient, PodbeanEpisodePublicationTracker podbeanEpisodePublicationTracker,
			MogulSecurityContexts securityContexts, IntegrationFlowContext integrationFlowContext) throws UnknownHostException {
		return new PodbeanPublicationWatcher(db, handler, podbeanEpisodePublicationTracker, integrationFlowContext, securityContexts,
				podbeanClient);
	}
}

/**
 * registers watches in the DB and ensures there's
 * an integration flow running somewhere to actually watch for that instance
 **/
@Service
@Transactional
class PodbeanPublicationWatcher {


	private final Logger log = LoggerFactory.getLogger(getClass());
	private final String nodeName = InetAddress.getLocalHost().getHostName();
	private final Object monitor = new Object();
	private final Map<Long, IntegrationFlowContext.IntegrationFlowRegistration> watchers = new ConcurrentHashMap<>();
	private final JdbcClient db;
	private final RowMapper<PodbeanPublicationTracker> rowMapper = new PodbeanPublicationTrackerRowMapper();
	private final PodbeanEpisodePublicationTracker podbeanEpisodePublicationTracker;
	private final ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler;
	private final PodbeanClient podbeanClient;
	private final IntegrationFlowContext context;

	private final MogulSecurityContexts mogulSecurityContexts;

	PodbeanPublicationWatcher(JdbcClient db, ApplicationEventPublishingMessageHandler handler,
							  PodbeanEpisodePublicationTracker t,
							  IntegrationFlowContext integrationFlowContex,
							  MogulSecurityContexts mogulSecurityContexts, PodbeanClient c) throws UnknownHostException {
		this.episodeSyncApplicationEventPublishingMessageHandler = handler;
		this.db = db;
		this.mogulSecurityContexts = mogulSecurityContexts;
		this.podbeanEpisodePublicationTracker = t;
		this.podbeanClient = c;
		this.context = integrationFlowContex;
	}

	private void refresh() {
		synchronized (this.monitor) {
			var sql = """
					select * from podbean_publication_tracker where node_id = ? and stopped  is not null 
					""";
			var trackers = this.db.sql(sql).params(this.nodeName).query(this.rowMapper).list();
			for (var tracker : trackers) {
				var podcastId = tracker.podcastId();
				if (!this.watchers.containsKey(podcastId)) {
					log.info("going to launch a watcher on node [" + tracker.nodeId() + "] for podcastId [" + podcastId + "]");
					var registration = this.launch(tracker.mogulId(), podcastId);
					this.watchers.put(podcastId, registration);
				}
			}
		}

	}

	private IntegrationFlowContext.IntegrationFlowRegistration launch(Long mogulId, Long podcastId) {

		var messageSource = new MessageSource<Boolean>() {
			@Override
			public Message<Boolean> receive() {

				var mogulPodcasts = """
						select podcast_id from podbean_publication_tracker where node_id = ? and stopped is not null 
						""";

				// just reset everything local just in case something get missed elsewhere
				for (var podcastId : db.sql(mogulPodcasts).param(nodeName).query((rs, i) -> rs.getLong("podcast_id")).list())
					stop(podcastId);

				// and started > ( (now() - INTERVAL '1 hour'))

				var sql = """
							select * from podbean_publication_tracker where 
							mogul_id = ? 
							and
							podcast_id = ?
							and 
							stopped is null 
						""";
				var trackerList = db.sql(sql).params(mogulId, podcastId).query(rowMapper).list();
				var shouldContinue = !trackerList.isEmpty() && trackerList.getFirst().continueTracking();
				return MessageBuilder.withPayload(shouldContinue).build();
			}
		};
		var integrationFlow = IntegrationFlow //
				.from(messageSource, pm -> pm.poller(p -> PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofSeconds(0))))//
				.filter((GenericSelector<Boolean>) source -> {
					var keepWatching = (null != source && source);
					log.info("there are [" + this.watchers.size() + "] watchers on the local node [" + nodeName + "]");
					if (!keepWatching)
						stop(podcastId);
					log.info("should watching for  mogul id [" + mogulId + "] and podcast id [" + podcastId + "] continue? [" + source + "]");
					return Boolean.TRUE.equals(source);
				})//
				.handle((payload, headers) -> {
					var authentication = mogulSecurityContexts.install(mogulId);
					Assert.notNull(authentication, "the authentication for the current mogul is null!");
					return podbeanClient.getAllEpisodes();
				}) //
				.transform((GenericTransformer<Collection<Episode>, Collection<PodbeanEpisodePublicationTracker.NewlyPublishedEpisode>>)
						podbeanEpisodePublicationTracker::identifyNewlyPublishedEpisodes).filter((GenericSelector<Collection<Episode>>) source -> !source.isEmpty()).split(new AbstractMessageSplitter() {
					@Override
					protected Object splitMessage(Message<?> message) {
						return message.getPayload();
					}
				})//
				.transform((GenericTransformer<PodbeanEpisodePublicationTracker.NewlyPublishedEpisode, PodbeanEpisodePublishedEvent>)
						source -> new PodbeanEpisodePublishedEvent(source.podcastId(), source.episode()))//
				.handle(episodeSyncApplicationEventPublishingMessageHandler)//
				.get();

		return this.context.registration(integrationFlow).register();
	}

	public void stop(Long podcastId) {
		if (watchers.containsKey(podcastId)) {
			var flow = watchers.remove(podcastId);
			if (null != flow) {
				flow.stop();
			}
		}
	}


	public void watch(long mogulId, long podcastId) throws Exception {

		var sql = """
				insert into podbean_publication_tracker (  node_id, mogul_id, continue_tracking, podcast_id, started, stopped )
				values (?, ?, ?, ?, ?, ? )
				on conflict on constraint  podbean_publication_tracker_podcast_id_key
				do nothing
				""";


		this.db.sql(sql).params(this.nodeName, mogulId, true, podcastId, new Date(), null).update();

		this.refresh();
	}

}

record PodbeanPublicationTracker(Long mogulId, Long podcastId, String nodeId, boolean continueTracking, Date started,
								 Date stopped) {
}

class PodbeanPublicationTrackerRowMapper implements RowMapper<PodbeanPublicationTracker> {

	@Override
	public PodbeanPublicationTracker mapRow(ResultSet rs, int rowNum) throws SQLException {
		return null;
	}
}
