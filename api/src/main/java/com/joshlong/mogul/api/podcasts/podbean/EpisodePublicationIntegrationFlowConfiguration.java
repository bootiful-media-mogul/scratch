package com.joshlong.mogul.api.podcasts.podbean;

import com.joshlong.podbean.Episode;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Collection;

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
		log.info("watch out world! we've just " + "published a new episode! [" + event + "]");
	}

	// todo we got it working to the point it gets to here.
	// the plan is to dynamically launch one of these flows for each mogul that has an
	// unpublished podcast in their account.
	// we have to mtch on the title of the podcast, so there fore the title of the podcast
	// will need to remain frozen until it's published.
	// theres got to be some durable state wherein we know which moguls have podcasts that
	// are as yet un-published and we kick off these flows
	// either at statup time or right after the publication process has sent this podcast
	// to podbean

	/*
	 * Run the following queries to see this flow in action: UPDATE PODCAST SET
	 * PODBEAN_DRAFT_CREATED = NULL, REVISION = NULL ; UPDATE PODBEAN_EPISODE SET
	 * PREVIOUSLY_PUBLISHED = FALSE , PUBLISHED = FALSE ;
	 */
	// todo we need to figure out how to give this logic a tenant / mogul context
	// it's using the `PodbeanClient` which in turn uses the `TokenProvider` which in turn
	// expects an authenticated user context somewhere.
	// @Bean
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
