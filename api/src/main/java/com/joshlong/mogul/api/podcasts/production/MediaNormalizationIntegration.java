package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.managedfiles.CommonMediaTypes;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.function.Function;

/**
 * we want a way to take any asset - be it a {@literal .wav} or a {@literal .png} - and
 * turn it into a {@literal  .mp3} and a 1MB-or-less {@literal .jpg} respectively.
 */
@Configuration
class MediaNormalizationIntegration {

	static final String MEDIA_NORMALIZATION_FLOW = "mediaNormalizationFlow";

	static final String REQUESTS = MEDIA_NORMALIZATION_FLOW + "Requests";

	@Bean(REQUESTS)
	DirectChannelSpec mediaNormalizationFlowRequests() {
		return MessageChannels.direct();
	}

	private ManagedFile readAndTransformManagedFile(ManagedFileService managedFileService, ManagedFile payload,
			Function<File, File> normalizer, MediaType mediaType) {
		var tmp = tempLocalFileForManagedFile(payload);
		dump(managedFileService.read(payload.id()), tmp);
		var newFile = normalizer.apply(tmp);
		var managedFile = managedFileService.createManagedFile(payload.mogulId(), payload.bucket(), payload.folder(),
				"normalized-" + payload.filename(), newFile.length(), mediaType);
		managedFileService.write(managedFile.id(), managedFile.filename(), mediaType, new FileSystemResource(newFile));
		return managedFile;
	}
	/*
	'title': 'Podcasts',
    'new-podcast': 'New Podcast',
    'new-podcast.title': 'title',
    'title.ai.prompt': `please help me take the following podcast title and make it more pithy and exciting!`,
    'new-podcast.submit': 'create a new podcast' ,
    'podcasts.delete':'delete' ,
    'podcasts.episodes' : 'episodes',
	* */

	private static File tempLocalFileForManagedFile(ManagedFile managedFile) {
		try {
			return File.createTempFile("managedFileNormalization", managedFile.filename());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void dump(Resource resource, File file) {
		try (var i = new BufferedInputStream(resource.getInputStream()); var o = new BufferedOutputStream(new FileOutputStream(file))) {
			FileCopyUtils.copy(i, o);
		} ///
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean(MEDIA_NORMALIZATION_FLOW)
	IntegrationFlow mediaNormalizationFlow(@Qualifier(REQUESTS) MessageChannel requests, ImageEncoder imageEncoder,
			AudioEncoder audioEncoder, ManagedFileService managedFileService) {
		var imgMediaType = MediaType.parseMediaType("image/*");
		return IntegrationFlow//
			.from(requests)//
			.handle((GenericHandler<ManagedFile>) (payload, headers) -> {
				var isImage = imgMediaType.isCompatibleWith(MediaType.parseMediaType(payload.contentType()));
				return (isImage)
						? readAndTransformManagedFile(managedFileService, payload, imageEncoder::encode,
								CommonMediaTypes.JPG)
						: readAndTransformManagedFile(managedFileService, payload, audioEncoder::encode,
								CommonMediaTypes.MP3);

			})
			.get();
	}

}
