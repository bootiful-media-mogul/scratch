package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.managedfiles.CommonMediaTypes;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.utils.FileUtils;
import com.joshlong.mogul.api.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.unit.DataSize;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * we want a way to take any asset - be it a {@literal .wav} or a {@literal .png} - and
 * turn it into a {@literal  .mp3} and a 1MB-or-less {@literal .jpg} respectively.
 */
@Configuration
class MediaNormalizationIntegration {

	static final String IMAGE_FLOW = "images";

	static final String AUDIO_FLOW = "audio";

	static final String MEDIA_NORMALIZATION_FLOW = "mediaNormalizationFlow";

	static final String REQUESTS = MEDIA_NORMALIZATION_FLOW + "Requests";

	@Bean(REQUESTS)
	DirectChannelSpec mediaNormalizationFlowRequests() {
		return MessageChannels.direct();
	}

	private ManagedFile readAndTransform(ManagedFileService managedFileService, ManagedFile payload,
			Function<File, File> normalizer, MediaType mediaType) {
		var tmp = tempLocalFileForManagedFile(payload);
		dump(managedFileService.read(payload.id()), tmp);
		var newFile = normalizer.apply(tmp);
		var managedFile = managedFileService.createManagedFile(payload.mogulId(), payload.bucket(), payload.folder(),
				"normalized-" + payload.filename(), newFile.length(), mediaType);
		managedFileService.write(managedFile.id(), managedFile.filename(), mediaType, new FileSystemResource(newFile));
		return managedFile;
	}

	@Bean(IMAGE_FLOW)
	IntegrationFlow imageMediaNormalizationFlow(ManagedFileService managedFileService, ImageEncoder encoder) {
		return flow -> flow.handle((GenericHandler<ManagedFile>) (payload,
				headers) -> readAndTransform(managedFileService, payload, encoder::encode, CommonMediaTypes.JPG));
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

	@Bean(AUDIO_FLOW)
	IntegrationFlow audioMediaNormalizationFlow(AudioEncoder audioEncoder, ManagedFileService managedFileService) {
		return flow -> flow.handle((GenericHandler<ManagedFile>) (payload,
				headers) -> readAndTransform(managedFileService, payload, audioEncoder::encode, CommonMediaTypes.MP3));
	}

	@Bean(MEDIA_NORMALIZATION_FLOW)
	IntegrationFlow mediaNormalizationFlow(@Qualifier(REQUESTS) MessageChannel requests,
			@Qualifier(AUDIO_FLOW) IntegrationFlow audio, @Qualifier(IMAGE_FLOW) IntegrationFlow image) {
		var imgMediaType = MediaType.parseMediaType("image/*");
		return IntegrationFlow//
			.from(requests)//
			.route(ManagedFile.class,
					(Function<ManagedFile, Object>) managedFile -> (imgMediaType
						.isCompatibleWith(MediaType.parseMediaType(managedFile.contentType()))) ? IMAGE_FLOW
								: AUDIO_FLOW,
					rs -> rs.subFlowMapping(IMAGE_FLOW, image)
						.subFlowMapping(AUDIO_FLOW, audio)
						.defaultOutputToParentFlow()) //
			.get();
	}

}
