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
 * the idea here is that we want a way to take any asset - be it a {@literal .wav} or a
 * {@literal .png} - and turn it into a {@literal  .mp3} and a 1MB-or-less
 * {@literal .jpg}.
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
		return IntegrationFlow.from(requests)
			.route(ManagedFile.class,
					(Function<ManagedFile, Object>) managedFile -> (imgMediaType
						.isCompatibleWith(MediaType.parseMediaType(managedFile.contentType()))) ? IMAGE_FLOW
								: AUDIO_FLOW,
					rs -> rs.subFlowMapping(IMAGE_FLOW, image)
						.subFlowMapping(AUDIO_FLOW, audio)
						.defaultOutputToParentFlow())
			.get();
	}

}

@Component
class ImageEncoder {

	private final Logger log = LoggerFactory.getLogger(ImageEncoder.class);

	public final static DataSize MAX_SIZE = DataSize.ofMegabytes(1);

	ImageEncoder() throws Exception {
		for (var tool : Set.of("convert", "identify"))
			try (var cmd = new ProcessBuilder().command(tool, "--version").start().getInputStream();
					var ow = new InputStreamReader(cmd)) {
				var cmdOutput = FileCopyUtils.copyToString(ow);
				log.debug(cmdOutput);
			}
	}

	public File encode(File path) {
		try {
			var output = isValidImage(path)
					? Files.copy(path.toPath(), new File(path.getParentFile(), "copy.jpg").toPath()).toFile()
					: scale(convertFileToJpeg(path));
			Assert.state(isValidSize(output),
					"the output image [" + path.getAbsolutePath() + "] must be of the right file size");
			log.debug("in: " + path.getAbsolutePath() + System.lineSeparator() + "out: " + output.getAbsolutePath()
					+ System.lineSeparator());
			return output;
		} //
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	private boolean isValidSize(File in) {
		return (in.length() <= MAX_SIZE.toBytes());
	}

	private File convertFileToJpeg(File in) throws Exception {
		if (isValidType(in))
			return in;
		var converted = FileUtils.createRelativeTempFile(in, ".jpg");
		var convert = new ProcessBuilder().command("convert", in.getAbsolutePath(), converted.getAbsolutePath())
			.start();
		Assert.state(convert.waitFor() == 0, "the process should exit normally");
		return converted;
	}

	private boolean isValidType(File in) {
		return in.getName().toLowerCase(Locale.ROOT).endsWith(".jpg");
	}

	private File scale(File file) throws Exception {
		var original = file.getAbsolutePath();
		var dest = FileUtils.createRelativeTempFile(file);
		var output = dest.getAbsolutePath();
		var quality = 100;
		var size = 0L;
		do {
			ProcessUtils.runCommand("convert", original, "-quality", String.valueOf(quality), output);
			size = Files.size(Paths.get(output));
			quality -= 5;
		}
		while (size > MAX_SIZE.toBytes() && quality > 0);

		return dest;
	}

	private boolean isValidImage(File f) {
		return isValidSize(f) && isValidType(f);
	}

}

@Component
class AudioEncoder {

	private final Logger log = LoggerFactory.getLogger(getClass());

	AudioEncoder() throws Exception {
		try (var cmd = new ProcessBuilder().command("ffmpeg", "-version").start().getInputStream();
				var ow = new InputStreamReader(cmd)) {
			var cmdOutput = FileCopyUtils.copyToString(ow);
			log.debug(cmdOutput);
		}
	}

	File encode(File input) {
		try {
			var mp3Ext = "mp3";
			if (input.getAbsolutePath().endsWith(mp3Ext))
				return input;
			var mp3 = FileUtils.createRelativeTempFile(input, "." + mp3Ext);
			ProcessUtils.runCommand("ffmpeg", "-i " + input.getAbsolutePath(), "-o " + mp3.getAbsolutePath());
			return mp3;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}