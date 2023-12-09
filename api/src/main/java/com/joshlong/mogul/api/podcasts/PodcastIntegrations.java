package com.joshlong.mogul.api.podcasts;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Configuration
public class PodcastIntegrations {

	public static final String PODCAST_HEADER = "podcast";

	public static final String HEADER_ARCHIVE = "archive";

	public static final String HEADER_RESOURCE_TYPE = "resource-type";

	public static final String IMAGE_NORMALIZATION_FLOW = "image-normalization-integration-flow";

	public static final String AUDIO_NORMALIZATION_FLOW = "audio-normalization-integration-flow";

	/**
	 * the integration flow allowing us to normalize images
	 */
	public static final String FLOW_MEDIA_NORMALIZATION = "media-normalization-integration-flow";

	public static final String FLOW_PROCESSOR = "processor-integration-flow";

	/**
	 * given valid {@literal .mp3}s and {@literal .jpg}s, and a valid {@literal  .xml}
	 * manifest, publish a podcast
	 */
	public static final String CHANNEL_PIPELINE_REQUESTS = "pipeline-requests-channel";

	public static final String CHANNEl_PROCESSOR_REQUESTS = "processor-requests-channel";

	/**
	 * replies
	 */
	public static final String CHANNEl_PROCESSOR_REPLIES = "processor-replies-channel";

	/**
	 * replies from the AMQP outbound gateway destined for the processor
	 */
	public static final String CHANNEL_PROCESSOR_AMQP_REPLIES = "processor-amqp-replies-channel";

	/**
	 * requests from the AMQP outbound gateway destined for the processor
	 */
	public static final String PROCESSOR_AMQP_REQUESTS_CHANNEl = "processor-amqp-requests-channel";

	@Bean
	@Qualifier(CHANNEl_PROCESSOR_REQUESTS)
	MessageChannelSpec<DirectChannelSpec, DirectChannel> processorRequestsMessageChannel() {
		return MessageChannels.direct();
	}

	@Bean
	@Qualifier(CHANNEl_PROCESSOR_REPLIES)
	MessageChannelSpec<DirectChannelSpec, DirectChannel> processorRepliesMessageChannel() {
		return MessageChannels.direct();
	}

	@Bean
	@Qualifier(PROCESSOR_AMQP_REQUESTS_CHANNEl)
	MessageChannelSpec<DirectChannelSpec, DirectChannel> processorAmqpRequestsMessageChannel() {
		return MessageChannels.direct();
	}

	@Bean
	@Qualifier(CHANNEL_PROCESSOR_AMQP_REPLIES)
	MessageChannelSpec<DirectChannelSpec, DirectChannel> processorAmqpRepliesMessageChannel() {
		return MessageChannels.direct();
	}

	@Bean
	@Qualifier(CHANNEL_PIPELINE_REQUESTS)
	MessageChannelSpec<DirectChannelSpec, DirectChannel> pipelineRequestsMessageChannel() {
		return MessageChannels.direct();
	}

	public static GenericHandler<Object> terminatingDebugHandler() {
		return terminatingDebugHandler(null);
	}

	public static GenericHandler<Object> terminatingDebugHandler(String header) {
		var log = LoggerFactory.getLogger(PodcastIntegrations.class);
		return (payload, headers) -> {
			var message = new StringBuilder();
			if (StringUtils.hasText(header))
				message.append(header.toUpperCase(Locale.ROOT));
			message.append(System.lineSeparator());
			message.append("---------------");
			message.append(System.lineSeparator());
			message.append(payload.toString());
			message.append(System.lineSeparator());
			headers
				.forEach((k, v) -> message.append("\t").append(k).append('=').append(v).append(System.lineSeparator()));
			message.append(System.lineSeparator());
			log.debug(message.toString());
			return null;
		};
	}

	public static GenericHandler<Object> debugHandler() {
		return debugHandler(null);
	}

	public static GenericHandler<Object> debugHandler(String header) {
		return (payload, headers) -> {
			terminatingDebugHandler(header).handle(payload, headers);
			return payload;
		};
	}

}
