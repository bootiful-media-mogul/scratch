package com.joshlong.mogul.api.old;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;

//@Configuration
public class PodcastIntegrations {

	public static final String PODCAST_HEADER = "podcast";

	public static final String HEADER_ARCHIVE = "archive";

	public static final String HEADER_RESOURCE_TYPE = "resource-type";

	/**
	 * the integration flow allowing us to normalize images
	 */

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

}
