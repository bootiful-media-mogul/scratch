package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.utils.IntegrationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.UUID;

/**
 * we have a python processor that will take the audio files we feed it and turn them into
 * a final, produced audio form. this is the integration that gets our
 * {@link com.joshlong.mogul.api.podcasts.Episode episodes} in and out of that processor.
 */
@Configuration
class ProductionIntegrationFlowConfiguration {

	private final Logger log = LoggerFactory.getLogger(getClass());

	static final String PRODUCTION_FLOW_REQUESTS = "podcast-episode-production-integration-flow-configuration";

	private final ManagedFileService managedFileService;

	ProductionIntegrationFlowConfiguration(ManagedFileService managedFileService) {
		this.managedFileService = managedFileService;
	}

	@Bean(PRODUCTION_FLOW_REQUESTS)
	DirectChannelSpec requests() {
		return MessageChannels.direct();
	}

	@Bean
	IntegrationFlow episodeProductionIntegrationFlow(ApiProperties properties, PodcastService podcastService,
			AmqpTemplate amqpTemplate, @Qualifier(PRODUCTION_FLOW_REQUESTS) MessageChannel channel) {
		var episodeIdHeaderName = "episodeId";
		var q = properties.podcasts().processor().amqp().requests();
		return IntegrationFlow.from(channel)//
			.handle(IntegrationUtils.debugHandler("got invoked by the gateway"))
			// todo send bumper s3 uris to the processor, so that they can be per-user.
			// folks would upload ManagedFiles for audio assets, and we'd pull 'em from
			// settings, i guess.
			// todo build a settings page and approach for podbean so users can specify
			// things like bumper music, podbean settings, etc.
			.transform(new AbstractTransformer() {
				@Override
				protected Object doTransform(Message<?> message) {
					Assert.state(message.getPayload() instanceof Episode, "the payload must be an instance of Episode");
					var source = (Episode) message.getPayload();
					var map = Map.of(//
							"introduction", source.producedIntroduction().s3Uri().toString(), //
							"interview", source.producedInterview().s3Uri().toString(), //
							"output", source.producedAudio().s3Uri().toString(), //
							"uid", UUID.randomUUID().toString(), //
							"episodeId", Long.toString(source.id())//
					);//
					return MessageBuilder.withPayload(map)
						.copyHeadersIfAbsent(message.getHeaders())
						.setHeader(episodeIdHeaderName, source.id())
						.build();
				}
			})
			.transform(new ObjectToJsonTransformer())
			.handle(IntegrationUtils.debugHandler("about to send the request out to AMQP"))
			.handle(Amqp.outboundGateway(amqpTemplate)//
				.routingKey(q)//
				.exchangeName(q)//
			)//
			.handle(IntegrationUtils.debugHandler("got a response from AMQP"))//
			.transform(new JsonToObjectTransformer(Map.class))//
			.handle(IntegrationUtils.debugHandler("turned the JSON from AMQP into a Map<K,V>"))//
			.handle((GenericHandler<Map<String, String>>) (payload, headers) -> {
				var managedFile = this.doWrite(episodeIdHeaderName, podcastService, managedFileService, headers,
						payload);//
				return MessageBuilder.withPayload(managedFile).copyHeadersIfAbsent(headers).build();
			})
			.get();
	}

	private ManagedFile doWrite(String episodeIdHeaderName, PodcastService podcastService,
			ManagedFileService managedFileService, Map<String, Object> headers, Map<String, String> mapPayload) {
		var episodeIdValue = headers.get(episodeIdHeaderName);
		var episodeId = episodeIdValue instanceof String episodeIdString ? //
				Long.parseLong(episodeIdString)//
				: ((Number) episodeIdValue).longValue();
		var episode = podcastService.getEpisodeById(episodeId);
		var producedAudio = episode.producedAudio();
		podcastService.writePodcastEpisodeProducedAudio(episode.id(), producedAudio.id());
		return managedFileService.getManagedFile(producedAudio.id());
	}

}
