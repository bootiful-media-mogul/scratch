package com.joshlong.mogul.api.podcasts;

import fm.bootifulpodcast.podbean.Episode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;

@Configuration
class PodcastPromotionIntegrationFlowConfiguration {

	@Bean
	ApplicationEventListeningMessageProducer podbeanEpisodePublishedEventApplicationEventListeningMessageProducer() {
		var eventListeningMessageProducer = new ApplicationEventListeningMessageProducer();
		eventListeningMessageProducer.setEventTypes(PodbeanEpisodePublishedEvent.class);
		return eventListeningMessageProducer;
	}

	@Bean
	IntegrationFlow publishedEpisodePromotionIntegrationFlow(PodcastRepository repository,
			ApplicationEventListeningMessageProducer applicationEventListeningMessageProducer) {
		return IntegrationFlow.from(applicationEventListeningMessageProducer)
			.transform((GenericTransformer<PodbeanEpisodePublishedEvent, Object>) PodbeanEpisodePublishedEvent::episode)
			.transform((GenericTransformer<Episode, Podcast>) source -> repository
				.podcastByPodbeanEpisodeId(source.getId())
				.iterator()
				.next())
			.handle((GenericHandler<Podcast>) (payload, headers) -> {
				repository.markAsNeedingPromotion(payload);
				return null;
			})
			.get();
	}

}
