package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.podcasts.PodbeanEpisodePublishedEvent;
import com.joshlong.mogul.api.utils.NodeUtils;
import com.joshlong.podbean.PodbeanClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Configuration
class PodbeanPublicationWatcherConfiguration {

	@Bean
	IntegrationFlow outstandingPublicationsIntegrationFlow(TaskExecutor applicationTaskExecutor,
			PodbeanPublicationWatcher watcher, MogulService mogulService) {
		return IntegrationFlow
			.from((MessageSource<Boolean>) () -> MessageBuilder.withPayload(Boolean.TRUE).build(),
					poller -> poller
						.poller(pollerFactory -> PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofMinutes(0))))
			.handle(message -> refresh(applicationTaskExecutor, mogulService, watcher))
			.get();
	}

	@Bean
	ApplicationEventPublishingMessageHandler episodeSyncApplicationEventPublishingMessageHandler() {
		var applicationEventPublishingMessageHandler = new ApplicationEventPublishingMessageHandler();
		applicationEventPublishingMessageHandler.setPublishPayload(true);
		return applicationEventPublishingMessageHandler;
	}

	private void refresh(TaskExecutor taskExecutor, MogulService service,
			PodbeanPublicationWatcher podbeanPublicationWatcher) {
		var outstandingPodbeanPublications = service.getOutstandingPodbeanPublications();
		for (var tracker : outstandingPodbeanPublications)
			if (!podbeanPublicationWatcher.isWatching(tracker.podcastId())
					&& NodeUtils.nodeId().equals(tracker.nodeId()))
				taskExecutor
					.execute(() -> podbeanPublicationWatcher.launch(tracker.mogulId(), tracker.podcastId()).start());
	}

	@Bean
	PodbeanPublicationWatcher podbeanPublicationWatcher(ApplicationEventPublishingMessageHandler handler,
			MogulService mogulService, PodbeanClient podbeanClient, MogulSecurityContexts mogulSecurityContexts,
			IntegrationFlowContext integrationFlowContext) {
		return new PodbeanPublicationWatcher(handler, podbeanClient, integrationFlowContext, mogulSecurityContexts,
				mogulService);
	}

	/*
	 * @Bean ApplicationEventListeningMessageProducer
	 * podbeanEpisodePublishedEventApplicationEventListeningMessageProducer() { var
	 * eventListeningMessageProducer = new ApplicationEventListeningMessageProducer();
	 * eventListeningMessageProducer.setEventTypes(PodbeanEpisodePublishedEvent.class);
	 * return eventListeningMessageProducer; }
	 */

	/**
	 * there could be many ways we get the {@link PodbeanEpisodePublishedEvent} event so
	 * best to decouple it
	 */
	/*
	 * @Bean IntegrationFlow publishedEpisodePromotionIntegrationFlow(MogulService
	 * mogulService, ApplicationEventListeningMessageProducer
	 * applicationEventListeningMessageProducer) { return IntegrationFlow//
	 * .from(applicationEventListeningMessageProducer)//
	 * .handle((GenericHandler<PodbeanEpisodePublishedEvent>) (payload, headers) -> { var
	 * podbeanEpisode = payload.episode(); var podcast = payload.podcast();
	 * mogulService.confirmPodbeanPublication(podcast, podbeanEpisode.getId()); return
	 * null; })// .get(); }
	 */

	@Component
	static class EpisodePublishedListener {

		private final MogulService mogulService;

		EpisodePublishedListener(MogulService mogulService) {
			this.mogulService = mogulService;
		}

		@EventListener
		void episodesPublished(PodbeanEpisodePublishedEvent podbeanEpisodePublishedEvent) {
			this.mogulService.confirmPodbeanPublication(podbeanEpisodePublishedEvent.podcast(),
					podbeanEpisodePublishedEvent.episode().getId());
		}

	}

}
