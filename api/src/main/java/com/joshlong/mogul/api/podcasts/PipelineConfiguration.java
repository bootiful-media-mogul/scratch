package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.*;
import com.joshlong.mogul.api.podcasts.archives.ArchiveResourceType;
import com.joshlong.mogul.api.utils.FileUtils;
import com.joshlong.mogul.api.utils.NodeUtils;
import com.joshlong.podbean.EpisodeStatus;
import com.joshlong.podbean.EpisodeType;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.serializer.Deserializer;
import org.springframework.http.MediaType;
import org.springframework.integration.core.GenericHandler;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.*;
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

	private final Logger log = LoggerFactory.getLogger(getClass());

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
				.setHeader(PodcastIntegrations.HEADER_ARCHIVE, archive)
				.setHeader(PodcastIntegrations.HEADER_RESOURCE_TYPE, type)
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
			try (var bufferedInputStream = new BufferedInputStream(new FileInputStream(source))) {
				return this.podcastArchiveDeserializer.deserialize(bufferedInputStream);
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
					var type = (ArchiveResourceType) m.getHeaders().get(PodcastIntegrations.HEADER_RESOURCE_TYPE);
					map.put(type, new FileSystemResource((File) m.getPayload()));
					var pa = (PodcastArchive) m.getHeaders().get(PodcastIntegrations.HEADER_ARCHIVE);
					title = pa.title();
					uuid = pa.uuid();
					description = pa.description();
				}
				return new PodcastArchive(uuid, title, description, map.get(ArchiveResourceType.INTRODUCTION),
						map.get(ArchiveResourceType.INTERVIEW), map.get(ArchiveResourceType.IMAGE));
			});
		}

	}

	private static final String MOGUL_ID_HEADER = "mogulId";

	private static final String AUTHENTICATION_HEADER = "Authorization";

	private static void doMove(Resource resource, File file) {
		try {
			try (var in = resource.getInputStream(); var out = new FileOutputStream(file)) {
				FileCopyUtils.copy(in, out);
			}
		}
		catch (Throwable t) {
			throw new RuntimeException("we couldn't copy the files!", t);
		}
	}

	@Bean
	IntegrationFlow pipeline(PodbeanClient podbeanClient, MogulService mogulService, ApiProperties properties,
			TransactionTemplate transactionTemplate, Storage storage,
			Deserializer<PodcastArchive> podcastArchiveDeserializer, MogulSecurityContexts mogulSecurityContexts,
			@Qualifier(PodcastIntegrations.CHANNEL_PIPELINE_REQUESTS) MessageChannel requests,
			@Qualifier(PodcastIntegrations.FLOW_PROCESSOR) IntegrationFlow processorIntegrationFlow,
			@Qualifier(PodcastIntegrations.FLOW_MEDIA_NORMALIZATION) IntegrationFlow mediaNormalizationIntegrationFlow) {
		return IntegrationFlow //
			.from(requests)//
			.transform(new FileToPodcastArchiveGenericTransformer(podcastArchiveDeserializer))
			.transform((GenericHandler<Object>) (payload, headers) -> {
				if (payload instanceof PodcastArchive podcastArchive) {
					log.info("launching the PodcastArchive with UID [" + podcastArchive.uuid() + "] on thread ["
							+ Thread.currentThread() + "]");
					var podcastDraftByUid = mogulService.getPodcastDraftByUid(podcastArchive.uuid());
					return MessageBuilder//
						.withPayload(payload)//
						.copyHeadersIfAbsent(headers)//
						.setHeader(MOGUL_ID_HEADER, podcastDraftByUid.mogulId())
						.build();
				}
				throw new IllegalStateException("the PodcastArchive is invalid");
			})
			.split(new ArchiveMediaSplitter())
			.transform(new FileSystemResourceToFileTransformer())
			.gateway(mediaNormalizationIntegrationFlow)
			.aggregate(new PodcastArchiveMediaAggregatorSpec())
			.transform(new HeaderFilter("sequenceNumber", "sequenceSize", "file_name", "correlationId",
					"file_originalFile", "file_relativePath"))
			.gateway(processorIntegrationFlow)
			.transform((GenericHandler<Object>) (payload, headers) -> {
				Assert.state(headers.containsKey(MOGUL_ID_HEADER), "there is not [" + MOGUL_ID_HEADER + "] header!");
				var authentication = mogulSecurityContexts.install((Long) headers.get(MOGUL_ID_HEADER));
				return MessageBuilder.withPayload(payload) //
					.copyHeadersIfAbsent(headers) //
					.setHeader(AUTHENTICATION_HEADER, authentication) //
					.build();
			})
			.transform(new HeaderFilter("sequenceNumber", "sequenceSize", "file_name", "correlationId",
					"json_resolvableType", "json__TypeId__", "sequenceSize", "resource-type", "file_originalFile",
					"file_relativePath"))
			.transform(PodcastIntegrations.debugHandler("processing reply from processor"))
			.transform(new JsonToObjectTransformer())
			.transform(new ProcessorReplyToDatabasePodcastGenericTransformer(mogulService))
			.handle(PodcastIntegrations.debugHandler(
					"got the reply from the processor, writing to the DB, going to send a request to Podbean"))
			.transform((GenericTransformer<Podcast, Podcast>) podcast -> {
				var podbeanDirectory = FileUtils
					.ensureDirectoryExists(properties.podcasts().pipeline().podbeanStaging());
				var mp3FileName = fileNameFor(podcast, "mp3");
				var mp3File = new File(podbeanDirectory, mp3FileName);

				doMove(storage.read(properties.podcasts().processor().s3().outputBucket(),
						podcast.uid() + "/podcast.mp3"), mp3File);

				var mp3Upload = podbeanClient.upload(MediaType.parseMediaType("audio/mpeg"), mp3File, mp3File.length());
				var jpgFileName = fileNameFor(podcast, "jpg");
				var jpgFile = new File(podbeanDirectory, jpgFileName);

				doMove(storage.read(properties.podcasts().processor().s3().inputBucket(), podcast.uid() + "/image.jpg"),
						jpgFile);

				var jpgUpload = podbeanClient.upload(MediaType.IMAGE_JPEG, jpgFile, jpgFile.length());
				var episode = podbeanClient.publishEpisode(podcast.title(), podcast.description(), EpisodeStatus.DRAFT,
						EpisodeType.PUBLIC, mp3Upload.getFileKey(), jpgUpload.getFileKey());
				log.info("the episode has been published to " + episode.toString() + '.');
				Assert.isTrue(mp3File.exists() && mp3File.delete(),
						"the" + " file " + mp3File.getAbsolutePath() + " does not exist or could not be deleted");

				transactionTemplate.execute(status -> {
					mogulService.connectPodcastToPodbeanPublication(podcast, episode.getId(), episode.getMediaUrl(),
							episode.getLogoUrl(), episode.getPlayerUrl());
					mogulService.monitorPodbeanPublication(NodeUtils.nodeId(), podcast);
					return null;
				});

				return podcast;
			})
			.handle(PodcastIntegrations.terminatingDebugHandler(
					"....at this point there's a separate integrationFlow that'll kick in once the podbean episode has been published"))
			.get();
	}

	private static String fileNameFor(Podcast podcast, String ext) {
		return podcast.uid() + "." + (ext.toLowerCase());
	}

	@Bean
	IntegrationFlow errorHandlingIntegrationFlow(@Qualifier(MessageHeaders.ERROR_CHANNEL) MessageChannel errors) {
		return IntegrationFlow.from(errors).handle(PodcastIntegrations.terminatingDebugHandler("error!")).get();
	}

	/**
	 * we get back a JSON map from the processor. we need to turn it again into a
	 * {@link PodcastArchive podcast archive}
	 */

	static class ProcessorReplyToDatabasePodcastGenericTransformer
			implements GenericTransformer<Map<String, Object>, Podcast> {

		private final Logger log = LoggerFactory.getLogger(getClass());

		private final MogulService mogulService;

		ProcessorReplyToDatabasePodcastGenericTransformer(MogulService mogulService) {
			this.mogulService = mogulService;
		}

		@Override
		public Podcast transform(Map<String, Object> source) {
			try {
				log.debug("getting the podcast with values [" + source + "]");
				var mogul = mogulService.getCurrentMogul();
				log.info("got the mogul in [" + getClass().getName() + "] : " + mogul.id());

				var exportedAudioS3Name = (String) source.get("exported-audio");
				var exportedAudioS3FileName = exportedAudioS3Name.substring(exportedAudioS3Name.lastIndexOf('/'));

				var exportedPhotoS3Name = (String) source.get("exported-photo");
				var exportedPhotoS3FileName = exportedPhotoS3Name.substring(exportedPhotoS3Name.lastIndexOf('/'));

				var podcast = new Podcast(mogul.id(), null, (String) source.get("uid"), new Date(),
						(String) source.get("description"), null, (String) source.get("title"),
						new Podcast.Podbean(null, null, null, null, null), null,
						new Podcast.S3(new Podcast.S3.Audio(new URI(exportedAudioS3Name), exportedAudioS3FileName),
								new Podcast.S3.Photo(new URI(exportedPhotoS3Name), exportedPhotoS3FileName)));

				return this.mogulService.addPodcastEpisode(mogul.id(), podcast);
			} //
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private static boolean isValidPodcastArchiveFile(File zip) {
		return zip != null && zip.isFile() && zip.getName().toLowerCase().endsWith(".zip");
	}

	@Bean
	IntegrationFlow archiveFilesIntegrationFlow(ApiProperties properties,
			@Qualifier(PodcastIntegrations.CHANNEL_PIPELINE_REQUESTS) MessageChannel requests) {
		var inboundFileAdapter = Files.inboundAdapter(properties.podcasts().pipeline().archives())//
			.filterFunction(PipelineConfiguration::isValidPodcastArchiveFile)
			.preventDuplicates(true)//
			.autoCreateDirectory(true);
		return IntegrationFlow //
			.from(inboundFileAdapter)
			.handle(PodcastIntegrations.debugHandler())//
			.channel(requests)//
			.get();
	}

	// https://developers.podbean.com/podbean-api-docs/#api-appendix-Podbean-API-Limit
	// i can do two requests per second nonstop

}
