package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.managedfiles.CommonMediaTypes;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.utils.FileUtils;
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
import org.springframework.util.FileCopyUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
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
	// todo is it possible to have some sort of dirty check? eg, if we have already
	// generated the produced audio, and we did it
	// _after_ we generated all the produced_(graphic|interview|introduction) files, then
	// surely we
	// can skip this step and just return immediately?

	/*
	 * @Bean IntegrationFlow integrationFlow( ManagedFileService mfs,
	 *
	 * @Qualifier(PRODUCTION_FLOW_REQUESTS) MessageChannel channel) { return
	 * IntegrationFlow .from(channel) .handle((payload, headers) -> { if (payload
	 * instanceof Episode ep) return doWrite( log, ) return null; }) .get(); }
	 */

	@Bean
	IntegrationFlow episodeProductionIntegrationFlow(ApiProperties properties, PodcastService podcastService,
			AmqpTemplate amqpTemplate, @Qualifier(PRODUCTION_FLOW_REQUESTS) MessageChannel channel) throws Exception {

		var episodeIdHeaderName = "episodeId";
		var q = properties.podcasts().processor().amqp().requests();
		return IntegrationFlow.from(channel)
			.handle(IntegrationUtils.debugHandler("got invoked by the gateway"))
			// todo teach the python processor about these headers
			// todo could the assets for the bumpers and so on just be s3 uris as
			// configuration settings in the settings table?
			// settings that we send along with the request? so the processor would know
			// nothing about our s3 assets
			// it would get ALL assets from the request itself, which is also nice because
			// we could rework it to support
			// other users one day, too
			.transform(new AbstractTransformer() {
				@Override
				protected Object doTransform(Message<?> message) {
					Assert.state(message.getPayload() instanceof Episode, "the payload must be an instance of Episode");
					var source = (Episode) message.getPayload();
					var map = Map.of("introduction", source.producedIntroduction().s3Uri().toString(), "interview",
							source.producedInterview().s3Uri().toString(), "output",
							source.producedAudio().s3Uri().toString(), "uid", UUID.randomUUID().toString(), "episodeId",
							Long.toString(source.id()));
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
			.handle(IntegrationUtils.debugHandler("got a response from AMQP"))
			.transform(new JsonToObjectTransformer(Map.class))
			.handle(IntegrationUtils.debugHandler("turned the JSOn from AMQP into a Map<K,V>"))
			.handle((GenericHandler<Map<String, String>>) (payload, headers) -> {
				var managedFile = doWrite(log, episodeIdHeaderName, podcastService, managedFileService, headers,
						payload);
				return MessageBuilder.withPayload(managedFile).copyHeadersIfAbsent(headers).build();
			})
			.get();
	}

	private static ManagedFile doWrite(Logger log, String episodeIdHeaderName, PodcastService podcastService,
			ManagedFileService managedFileService, Map<String, Object> headers, Map<String, String> mapPayload) {
		var tmp = FileUtils.tempFile();
		Assert.notNull(tmp, "the temp directory must be non-null");
		var uri = URI.create(mapPayload.get("exported-audio"));
		var episodeIdValue = headers.get(episodeIdHeaderName);
		var episodeId = episodeIdValue instanceof String episodeIdString ? Long.parseLong(episodeIdString)
				: ((Number) episodeIdValue).longValue();
		Assert.state(episodeId != -1, "the episode ID should have been resolved");
		var episode = podcastService.getEpisodeById(episodeId);
		Assert.notNull(episode, "the episode ID should return a valid Episode");
		Assert.notNull(uri, "the returned URI should not be null");
		var producedAudio = episode.producedAudio();
		var resource = managedFileService.read(producedAudio.id());
		try {
			try (var in = new BufferedInputStream(resource.getInputStream());
					var out = new BufferedOutputStream(new FileOutputStream(tmp))) {
				log.debug("starting download to local file [" + tmp.getAbsolutePath() + "]");
				FileCopyUtils.copy(in, out);
				log.debug("finished download to local file [" + tmp.getAbsolutePath() + "]");
			} //

			log.debug("starting re-upload [" + producedAudio + ']');
			managedFileService.write(producedAudio.id(), producedAudio.filename(), CommonMediaTypes.MP3, tmp);
			log.debug("finished re-upload [" + producedAudio + ']');
		} //
		catch (IOException e) {
			throw new RuntimeException("could not download and then re-upload the file [" + tmp.getAbsolutePath()
					+ "] for episode [" + episode + "]", e);
		} //
		finally {
			FileUtils.delete(tmp);
		}
		Assert.notNull(producedAudio, "the produced audio file should never be null");
		return managedFileService.getManagedFile(producedAudio.id());
	}

}
