package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.podcasts.PodbeanEpisodePublishedEvent;
import com.joshlong.mogul.api.utils.NodeUtils;
import com.joshlong.podbean.PodbeanClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

@Configuration
class PodbeanPublicationWatcherConfiguration {

	@Bean
	IntegrationFlow outstandingPublicationsIntegrationFlow(TaskExecutor applicationTaskExecutor,
			PodbeanPublicationWatcher watcher, MogulService mogulService) {
		return IntegrationFlow
			.from((MessageSource<Long>) () -> MessageBuilder.withPayload(System.currentTimeMillis()).build(),
					poller -> poller
						.poller(pollerFactory -> PollerFactory.fixedRate(Duration.ofMinutes(1), Duration.ofMinutes(0))))
			.handle((payload, headers) -> {
				refresh(applicationTaskExecutor, mogulService, watcher);
				return null;
			})
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

	@Bean
	ApplicationEventListeningMessageProducer podbeanEpisodePublishedEventApplicationEventListeningMessageProducer() {
		var producer = new ApplicationEventListeningMessageProducer();
		producer.setEventTypes(PodbeanEpisodePublishedEvent.class);
		return producer;
	}

	@Bean
	IntegrationFlow episodePublishedIntegrationFlow(
			ApplicationEventListeningMessageProducer podbeanEpisodePublishedEventApplicationEventListeningMessageProducer,
			MogulService mogulService) {

		return IntegrationFlow.from(podbeanEpisodePublishedEventApplicationEventListeningMessageProducer)
			.handle((GenericHandler<PodbeanEpisodePublishedEvent>) (payload, headers) -> {
				var episode = payload.episode();
				mogulService.confirmPodbeanPublication(payload.podcast(), episode.getPermalinkUrl(),
						episode.getDuration());
				return null;
			})
			.get();
	}

}
