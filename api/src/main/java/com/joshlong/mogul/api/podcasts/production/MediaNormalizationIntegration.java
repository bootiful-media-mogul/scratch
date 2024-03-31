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

	public static final String MEDIA_NORMALIZATION_FLOW = "mediaNormalizationFlow";

	static final String MEDIA_NORMALIZATION_FLOW_CHANNEL = MEDIA_NORMALIZATION_FLOW + "Requests";

	@Bean(MEDIA_NORMALIZATION_FLOW_CHANNEL)
	DirectChannelSpec mediaNormalizationFlowRequests() {
		return MessageChannels.direct();
	}

	private MediaNormalizationIntegrationResponse readAndTransformManagedFile(ManagedFileService managedFileService,
			ManagedFile input, Function<File, File> normalizer, MediaType mediaType, ManagedFile output) {
		var tmp = tempLocalFileForManagedFile(input);
		dump(managedFileService.read(input.id()), tmp);
		var newFile = normalizer.apply(tmp);
		managedFileService.write(output.id(), output.filename(), mediaType, new FileSystemResource(newFile));
		return new MediaNormalizationIntegrationResponse(input, output);
	}

	private static File tempLocalFileForManagedFile(ManagedFile managedFile) {
		try {
			return File.createTempFile("managedFileNormalization", managedFile.filename());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void dump(Resource resource, File file) {
		try (var i = new BufferedInputStream(resource.getInputStream());
				var o = new BufferedOutputStream(new FileOutputStream(file))) {
			FileCopyUtils.copy(i, o);
		} ///
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean(MEDIA_NORMALIZATION_FLOW)
	IntegrationFlow mediaNormalizationFlow(@Qualifier(MEDIA_NORMALIZATION_FLOW_CHANNEL) MessageChannel requests,
			ImageEncoder imageEncoder, AudioEncoder audioEncoder, ManagedFileService managedFileService) {
		var imgMediaType = MediaType.parseMediaType("image/*");
		return IntegrationFlow//
			.from(requests)//
			.handle((GenericHandler<MediaNormalizationIntegrationRequest>) (io, headers) -> {
				var input = io.input();
				var output = io.output();
				var isImage = imgMediaType.isCompatibleWith(MediaType.parseMediaType(input.contentType()));
				return (isImage)
						? readAndTransformManagedFile(managedFileService, input, imageEncoder::encode,
								CommonMediaTypes.JPG, output)
						: readAndTransformManagedFile(managedFileService, input, audioEncoder::encode,
								CommonMediaTypes.MP3, output);

			})
			.get();
	}

}
