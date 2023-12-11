package com.joshlong.mogul.api.old.media;

import com.joshlong.mogul.api.old.PodcastIntegrations;
import com.joshlong.mogul.api.old.archives.ArchiveResourceType;
import com.joshlong.mogul.api.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

import static com.joshlong.mogul.api.old.PodcastIntegrations.*;

/**
 * the idea here is that we want a way to take any asset - be it a {@literal .wav} or a
 * {@literal .png} - and turn it into a {@literal  .mp3} and a 1MB-or-less
 * {@literal .jpg}.
 */
//@Configuration
class MediaNormalizationIntegration {

	@Bean(AUDIO_NORMALIZATION_FLOW)
	IntegrationFlow audioNormalizationFlow() {
		return flow -> flow.handle(PodcastIntegrations.debugHandler("audio flow"));
	}

	@Bean(IMAGE_NORMALIZATION_FLOW)
	IntegrationFlow imageNormalizationFlow(@Qualifier(ImageEncoder.IMAGE_ENCODER) MediaEncoder imageEncoder) {
		return flow -> flow.handle(PodcastIntegrations.debugHandler("image flow"))
			.transform(File.class, imageEncoder::encode);
	}

	@Bean(name = FLOW_MEDIA_NORMALIZATION)
	IntegrationFlow mediaNormalizationFlow(@Qualifier(IMAGE_NORMALIZATION_FLOW) IntegrationFlow images,
			@Qualifier(AUDIO_NORMALIZATION_FLOW) IntegrationFlow audio) {
		return flow -> flow.route(Message.class, this::destinationForMessage,
				m -> m.subFlowMapping(ArchiveResourceType.INTERVIEW, audio)
					.subFlowMapping(ArchiveResourceType.INTRODUCTION, audio)
					.subFlowMapping(ArchiveResourceType.IMAGE, images)
					.defaultOutputToParentFlow());
	}

	private ArchiveResourceType destinationForMessage(Message<?> message) {
		Assert.state(
				message.getHeaders().containsKey(HEADER_RESOURCE_TYPE)
						&& message.getHeaders().get(HEADER_RESOURCE_TYPE) instanceof ArchiveResourceType,
				() -> "there should be a header [" + HEADER_RESOURCE_TYPE + "]!");
		return (ArchiveResourceType) message.getHeaders().get(HEADER_RESOURCE_TYPE);
	}

}

interface MediaEncoder {

	File encode(File input);

}

@Component(ImageEncoder.IMAGE_ENCODER)
class ImageEncoder implements MediaEncoder {

	static final String IMAGE_ENCODER = "image-encoder";

	private final static Logger log = LoggerFactory.getLogger(ImageEncoder.class);

	public final static DataSize MAX_SIZE = DataSize.ofMegabytes(1);

	ImageEncoder() throws Exception {
		for (var tool : Set.of("convert", "identify"))
			try (var cmd = new ProcessBuilder().command(tool, "--version").start().getInputStream();
					var ow = new InputStreamReader(cmd)) {
				var cmdOutput = FileCopyUtils.copyToString(ow);
				log.debug(cmdOutput);
			}

	}

	@Override
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
		var converted = FileUtils.createRelativeTempFile(in);
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
			runCommand("convert", original, "-quality", String.valueOf(quality), output);
			size = Files.size(Paths.get(output));
			quality -= 5;
		}
		while (size > MAX_SIZE.toBytes() && quality > 0);

		return dest;
	}

	private static void runCommand(String... command) throws IOException, InterruptedException {
		var processBuilder = new ProcessBuilder(command);
		var process = processBuilder.start();
		process.waitFor();
	}

	private boolean isValidImage(File f) {
		return isValidSize(f) && isValidType(f);
	}

}
