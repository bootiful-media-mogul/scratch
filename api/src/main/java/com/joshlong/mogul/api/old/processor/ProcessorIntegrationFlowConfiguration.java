package com.joshlong.mogul.api.old.processor;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.Storage;
import com.joshlong.mogul.api.old.PodcastArchive;
import com.joshlong.mogul.api.old.PodcastIntegrations;
import com.joshlong.mogul.api.old.archives.ArchiveResourceType;
import com.joshlong.mogul.api.utils.IntegrationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * this handles the integration with the external Python multiplexer
 */
// @Configuration
// @IntegrationComponentScan
class ProcessorIntegrationFlowConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ProcessorIntegrationFlowConfiguration.class);

	private static final String PROCESSOR_OUTBOUND_MESSAGE_CHANNEL = "processorOutboundMessageChannel";

	private static final String PROCESSOR_INBOUND_MESSAGE_CHANNEL = "processorInboundMessageChannel";

	private S3UploadRequest s3UploadRequestForResource(PodcastArchive archive, String bucket, ArchiveResourceType type,
			Resource resource) {

		var fileName = switch (type) {
			case IMAGE -> "image.jpg";
			case INTERVIEW -> "interview.mp3";
			case INTRODUCTION -> "introduction.mp3";
			case MANIFEST -> "manifest.xml";
		};
		try {
			return new S3UploadRequest(archive.uuid(), resource, bucket, archive.uuid(), fileName);
		} //
		catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	@Bean(PodcastIntegrations.FLOW_PROCESSOR)
	IntegrationFlow outboundProcessorFlow(Storage storage, ProcessorClient processorClient, ApiProperties properties) {

		return flow -> flow//
			.split(new AbstractMessageSplitter() {
				@Override
				protected Object splitMessage(Message<?> message) {
					if (message.getPayload() instanceof PodcastArchive podcastArchive) {
						var set = new HashSet<Message<S3UploadRequest>>();
						var map = Map.of(ArchiveResourceType.INTERVIEW, podcastArchive.interview(), //
								ArchiveResourceType.INTRODUCTION, podcastArchive.introduction(), //
								ArchiveResourceType.IMAGE, podcastArchive.image()//
						);
						for (var k : map.keySet()) {
							var s3UploadRequest = s3UploadRequestForResource(podcastArchive,
									properties.podcasts().processor().s3().inputBucket(), k, map.get(k));
							set.add(MessageBuilder.withPayload(s3UploadRequest)
								.setHeader(PodcastIntegrations.HEADER_RESOURCE_TYPE, k)
								.copyHeadersIfAbsent(message.getHeaders())
								.build());
						}
						return set;
					}
					throw new IllegalStateException("the input payload should be a " + PodcastArchive.class.getName()
							+ " but was [" + message.getPayload() + "]");
				}
			})//
			.transform((GenericHandler<S3UploadRequest>) (payload, headers) -> {
				storage.write(payload.bucket(), payload.folder() + '/' + payload.name(), payload.resource());
				return MessageBuilder
					.withPayload(new S3UploadResponse(payload, payload.folder(),
							URI.create("s3://" + payload.bucket() + '/' + payload.folder() + '/' + payload.name())))
					.copyHeadersIfAbsent(headers)
					.build();
			})//
			.aggregate(aggregatorSpec -> aggregatorSpec.outputProcessor(group -> {
				var messages = group.getMessages();
				Assert.state(messages != null && !messages.isEmpty(), "there should be messages");
				var payloadArchive = (PodcastArchive) messages.iterator()
					.next()
					.getHeaders()
					.get(PodcastIntegrations.HEADER_ARCHIVE);
				Assert.notNull(payloadArchive, "the podcastArchive must be non-null ");
				var s3UploadResponseHashMap = new HashMap<ArchiveResourceType, S3UploadResponse>();
				for (var message : messages) {
					var payload = (S3UploadResponse) message.getPayload();
					var headers = message.getHeaders();
					s3UploadResponseHashMap
						.put((ArchiveResourceType) headers.get(PodcastIntegrations.HEADER_RESOURCE_TYPE), payload);
				}
				return new S3UploadedPodcastArchive(payloadArchive, s3UploadResponseHashMap);
			}))//
			.transform(S3UploadedPodcastArchive.class, new S3UploadedPodcastArchiveToProcessorRequestGenericConverter())//
			.transform(new ObjectToJsonTransformer())//
			.handle((GenericHandler<String>) (payload, headers) -> processorClient.process(payload));//
	}

	@Bean(name = PROCESSOR_INBOUND_MESSAGE_CHANNEL)
	DirectChannelSpec processorInboundMessageChannel() {
		return MessageChannels.direct();
	}

	@Bean(name = PROCESSOR_OUTBOUND_MESSAGE_CHANNEL)
	DirectChannelSpec processorOutboundMessageChannel() {
		return MessageChannels.direct();
	}

	// @MessagingGateway
	public interface ProcessorClient {

		@Gateway(requestChannel = PROCESSOR_OUTBOUND_MESSAGE_CHANNEL, replyChannel = PROCESSOR_INBOUND_MESSAGE_CHANNEL)
		String process(@Payload String input);

	}

	@Bean
	IntegrationFlow outboundAmqpIntegrationFlow(ApiProperties properties,
			@Qualifier(PROCESSOR_OUTBOUND_MESSAGE_CHANNEL) MessageChannel outbound,
			@Qualifier(PROCESSOR_INBOUND_MESSAGE_CHANNEL) MessageChannel inbound, AmqpTemplate amqpTemplate) {
		var q = properties.podcasts().processor().amqp().requests();
		return IntegrationFlow.from(outbound)
			.transform((GenericTransformer<String, Map<String, String>>) source -> Map.of("request", source))
			.transform(new ObjectToJsonTransformer())
			.handle(Amqp.outboundGateway(amqpTemplate)//
				.routingKey(q)//
				.exchangeName(q)//
			)
			.handle(IntegrationUtils.debugHandler("outbound processor AMQP message"))
			.channel(inbound)
			.get();
	}
	//

	record S3UploadedPodcastArchive(PodcastArchive podcastArchive,
			Map<ArchiveResourceType, S3UploadResponse> s3Uploads) {
	}

	record S3UploadRequest(String key, Resource resource, String bucket, String folder, String name) {
	}

	record S3UploadResponse(S3UploadRequest request, String key, URI s3Uri) {
	}

	record ProcessorRequest(String uid, String title, String description, Map<String, String> uploads) {
	}

	static class S3UploadedPodcastArchiveToProcessorRequestGenericConverter
			implements GenericTransformer<S3UploadedPodcastArchive, ProcessorRequest> {

		@Override
		public ProcessorRequest transform(S3UploadedPodcastArchive s3UploadedPodcastArchive) {
			var podcastArchive = s3UploadedPodcastArchive.podcastArchive();
			var uploads = new HashMap<String, String>();
			for (var type : s3UploadedPodcastArchive.s3Uploads().keySet()) {
				uploads.put(type.name(), s3UploadedPodcastArchive.s3Uploads().get(type).s3Uri().toString());
			}
			return new ProcessorRequest(podcastArchive.uuid(), podcastArchive.title(), podcastArchive.description(),
					uploads);
		}

	}

}
