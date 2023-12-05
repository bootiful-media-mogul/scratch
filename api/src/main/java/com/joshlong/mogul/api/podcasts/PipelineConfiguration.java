package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.mogul.api.podcasts.archives.ArchiveResourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

/**
 * todo rip the processor pipeline out of the main one; it shouldn't be a gateway. just
 * make it a regular flow so that i can not worry about the response being delayed and
 * thus lost
 *
 * todo make sure to launch podbean and then update the record in the table; isd there
 * some sort of id we get back from podbean that we can then use to see if any changes
 * have been made to it and update accordingly?
 */

@Configuration
class PipelineConfiguration {

	static class ArchiveMediaSplitter extends AbstractMessageSplitter {

		@Override
		protected Object splitMessage(Message<?> message) {
			if (message.getPayload() instanceof PodcastArchive podcastArchive) {
				var map = Map.of(//
						ArchiveResourceType.INTERVIEW, podcastArchive.interview(), //
						ArchiveResourceType.INTRODUCTION, podcastArchive.introduction(), //
						ArchiveResourceType.IMAGE, podcastArchive.image()); //
				var set = new HashSet<Message<Resource>>();
				for (var e : map.entrySet())
					set.add(resourceMessage(podcastArchive, e.getKey(), e.getValue()));
				return set;
			}
			else
				throw new IllegalStateException("the payload should be a type of " + PodcastArchive.class.getName());
		}

		private static Message<Resource> resourceMessage(PodcastArchive archive, ArchiveResourceType type,
				Resource resource) {
			return MessageBuilder.withPayload(resource)
				.setHeader(Integrations.HEADER_ARCHIVE, archive)
				.setHeader(Integrations.HEADER_RESOURCE_TYPE, type)
				.build();
		}

	}

	static class FileSystemResourceToFileTransformer implements GenericTransformer<Resource, File> {

		@Override
		public File transform(Resource source) {
			try {
				return source.getFile();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	static class FileToPodcastArchiveGenericTransformer implements GenericTransformer<File, PodcastArchive> {

		private final Deserializer<PodcastArchive> podcastArchiveDeserializer;

		FileToPodcastArchiveGenericTransformer(Deserializer<PodcastArchive> podcastArchiveDeserializer) {
			this.podcastArchiveDeserializer = podcastArchiveDeserializer;
		}

		@Override
		public PodcastArchive transform(File source) {
			try (var i = new FileInputStream(source)) {
				return this.podcastArchiveDeserializer.deserialize(i);
			} //
			catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

	}

	static class PodcastArchiveMediaAggregatorSpec implements Consumer<AggregatorSpec> {

		@Override
		public void accept(AggregatorSpec aggregatorSpec) {
			aggregatorSpec.outputProcessor(group -> {
				var map = new HashMap<ArchiveResourceType, Resource>();
				var messages = group.getMessages();
				var uuid = (String) null;
				var title = (String) null;
				var description = (String) null;
				for (var m : messages) {
					var type = (ArchiveResourceType) m.getHeaders().get(Integrations.HEADER_RESOURCE_TYPE);
					map.put(type, new FileSystemResource((File) m.getPayload()));
					var pa = (PodcastArchive) m.getHeaders().get(Integrations.HEADER_ARCHIVE);
					title = pa.title();
					uuid = pa.uuid();
					description = pa.description();
				}
				return new PodcastArchive(uuid, title, description, map.get(ArchiveResourceType.INTRODUCTION),
						map.get(ArchiveResourceType.INTERVIEW), map.get(ArchiveResourceType.IMAGE));
			});
		}

	}

	@Bean
	IntegrationFlow pipeline(MogulService mogulService, Deserializer<PodcastArchive> podcastArchiveDeserializer,
			@Qualifier(Integrations.CHANNEL_PIPELINE_REQUESTS) MessageChannel requests,
			@Qualifier(Integrations.FLOW_PROCESSOR) IntegrationFlow processorIntegrationFlow,
			@Qualifier(Integrations.FLOW_MEDIA_NORMALIZATION) IntegrationFlow mediaNormalizationIntegrationFlow) {

		return IntegrationFlow.from(requests)
			.transform(new FileToPodcastArchiveGenericTransformer(podcastArchiveDeserializer))
			.split(new ArchiveMediaSplitter())
			.transform(new FileSystemResourceToFileTransformer())
			.gateway(mediaNormalizationIntegrationFlow)
			.aggregate(new PodcastArchiveMediaAggregatorSpec())
			.transform(new HeaderFilter("sequenceNumber", "sequenceSize", "file_name", "correlationId",
					"file_originalFile", "file_relativePath"))
			.gateway(processorIntegrationFlow)
			.transform(new HeaderFilter("sequenceNumber", "sequenceSize", "file_name", "correlationId",
					"json_resolvableType", "json__TypeId__", "sequenceSize", "resource-type", "file_originalFile",
					"file_relativePath"))
			.transform(Integrations.debugHandler("processing reply from processor"))
			.transform(new JsonToObjectTransformer())
			.transform(new ProcessorReplyToDatabasePodcastGenericTransformer(mogulService))
			.handle(Integrations.debugHandler(
					"got the reply from the processor, writing to the DB, going to send a request to Podbean"))
			.handle(Integrations.terminatingDebugHandler(
					"....at this point there's a separate integrationFlow that'll kick in once the podbean episode has been published"))
			// // this should only happen _after_ the publication event has been fired
			// .handle(Integrations.debugHandler("going to send social media promotion via
			// the social service"))
			// .handle(Integrations.debugHandler("going to publish a blog via some blog
			// service i have yet to build"))
			.get();
	}

	@Bean
	IntegrationFlow errorHandlingIntegrationFlow(@Qualifier(MessageHeaders.ERROR_CHANNEL) MessageChannel errors) {
		return IntegrationFlow.from(errors).handle(Integrations.terminatingDebugHandler("error!")).get();
	}

	/**
	 * we get back a JSON map from the processor. we need to turn it again into a
	 * {@link PodcastArchive podcast archive}
	 */

	static class ProcessorReplyToDatabasePodcastGenericTransformer
			implements GenericTransformer<Map<String, Object>, Podcast> {

		private final MogulService mogulService;

		ProcessorReplyToDatabasePodcastGenericTransformer(MogulService mogulService) {
			this.mogulService = mogulService;
		}

		@Override
		public Podcast transform(Map<String, Object> source) {
			try {

				var mogulName = SecurityContextHolder.getContext().getAuthentication().getName();
				var mogul = mogulService.getMogulByName(mogulName);

				var exportedAudioS3Name = (String) source.get("exported-audio");
				var exportedAudioS3FileName = exportedAudioS3Name.substring(exportedAudioS3Name.lastIndexOf('/'));

				var exportedPhotoS3Name = (String) source.get("exported-photo");
				var exportedPhotoS3FileName = exportedPhotoS3Name.substring(exportedPhotoS3Name.lastIndexOf('/'));

				var podcast = new Podcast(mogul.id(), null, (String) source.get("uid"), new Date(),
						(String) source.get("description"), null, (String) source.get("title"),
						new Podcast.Podbean(null, null, null, null, null), null,
						new Podcast.S3(new Podcast.S3.Audio(new URI(exportedAudioS3Name), exportedAudioS3FileName),
								new Podcast.S3.Photo(new URI(exportedPhotoS3Name), exportedPhotoS3FileName)));

				this.mogulService.addPodcastEpisode(mogul.id(), podcast);

				return podcast;

			} //
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}

		}

	}

	@Bean
	IntegrationFlow archiveFilesIntegrationFlow(ApiProperties properties,
			@Qualifier(Integrations.CHANNEL_PIPELINE_REQUESTS) MessageChannel requests) {
		var inboundFileAdapter = Files.inboundAdapter(properties.podcasts().pipeline().archives())
			.autoCreateDirectory(true);
		return IntegrationFlow.from(inboundFileAdapter).handle(Integrations.debugHandler()).channel(requests).get();
	}

	//
	// https://developers.podbean.com/podbean-api-docs/#api-appendix-Podbean-API-Limit
	// i can do two requests per second nonstop

}
