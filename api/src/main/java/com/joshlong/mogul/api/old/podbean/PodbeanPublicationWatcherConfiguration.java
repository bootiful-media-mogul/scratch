package com.joshlong.mogul.api.old.podbean;

//@Configuration
class PodbeanPublicationWatcherConfiguration {
/*
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

		return IntegrationFlow//
			.from(podbeanEpisodePublishedEventApplicationEventListeningMessageProducer)
			.handle((GenericHandler<PodbeanEpisodePublishedEvent>) (payload, headers) -> {
				var episode = payload.episode();
				mogulService.confirmPodbeanPublication(payload.podcast(), episode.getPermalinkUrl(),
						episode.getDuration());
				return null;
			})
			.get();
	}*/

}
