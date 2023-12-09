package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.podcasts.PodbeanEpisodePublishedEvent;
import com.joshlong.mogul.api.podcasts.PodcastIntegrations;
import com.joshlong.mogul.api.utils.NodeUtils;
import com.joshlong.podbean.EpisodeStatus;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * registers watches in the DB and ensures there's an integration flow running somewhere
 * to actually watch for that instance
 **/
@Service
class PodbeanPublicationWatcher {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<Long, IntegrationFlowContext.IntegrationFlowRegistration> watchers = new ConcurrentHashMap<>();

	private final ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler;

	private final PodbeanClient podbeanClient;

	private final IntegrationFlowContext context;

	private final MogulSecurityContexts mogulSecurityContexts;

	private final MogulService mogulService;

	PodbeanPublicationWatcher(
			ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler,
			PodbeanClient podbeanClient, IntegrationFlowContext context, MogulSecurityContexts mogulSecurityContexts,
			MogulService mogulService) {
		this.episodeSyncApplicationEventPublishingMessageHandler = episodeSyncApplicationEventPublishingMessageHandler;
		this.podbeanClient = podbeanClient;
		this.context = context;
		this.mogulSecurityContexts = mogulSecurityContexts;
		this.mogulService = mogulService;
	}

	public boolean isWatching(Long podcastId) {
		return this.watchers.containsKey(podcastId);
	}

	public IntegrationFlowContext.IntegrationFlowRegistration launch(Long mogulId, Long podcastId) {

		var messageSource = new MessageSource<Boolean>() {
			@Override
			public Message<Boolean> receive() {
				for (var podbeanPublication : mogulService.getPodbeanPublicationsByNode(NodeUtils.nodeId()))
					if (!podbeanPublication.continueTracking())
						stop(podbeanPublication.podcastId());
				var shouldContinue = mogulService.getPodbeanPublicationByPodcast(mogulService.getPodcastById(podcastId))
					.continueTracking();
				return MessageBuilder.withPayload(shouldContinue)
					.setHeader(PodcastIntegrations.PODCAST_HEADER, podcastId)
					.build();
			}
		};
		var integrationFlow = IntegrationFlow //
			.from(messageSource,
					pm -> pm.poller(p -> PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofSeconds(0))))//
			.transform((GenericTransformer<Boolean, PodbeanEpisodePublishedEvent>) source -> {
				var authentication = mogulSecurityContexts.install(mogulId);
				Assert.notNull(authentication, "the authentication for the current mogul is null!");
				var episodes = podbeanClient.getAllEpisodes();
				var podcast = mogulService.getPodcastById(podcastId);
				for (var e : episodes) {
					var matches = e.getTitle().equalsIgnoreCase(podcast.title());
					var published = StringUtils.hasText(e.getStatus())
							&& e.getStatus().equalsIgnoreCase(EpisodeStatus.PUBLISH.name());
					if (matches && published) {
						return new PodbeanEpisodePublishedEvent(podcast, e);
					}
				}
				return null;
			}) //
			.handle(episodeSyncApplicationEventPublishingMessageHandler)//
			.get();

		var registration = this.context.registration(integrationFlow).register();
		this.watchers.put(podcastId, registration);
		return registration;
	}

	public void stop(Long podcastId) {
		log.info("stopping " + podcastId + ".");
		if (watchers.containsKey(podcastId)) {
			var flow = watchers.remove(podcastId);
			if (null != flow) {
				flow.stop();
			}
		}
	}

}
