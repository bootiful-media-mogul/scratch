package com.joshlong.mogul.api.podcasts.media;


import com.joshlong.mogul.api.podcasts.Integrations;
import com.joshlong.mogul.api.podcasts.archives.ArchiveResourceType;
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

import java.awt.*;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.joshlong.mogul.api.podcasts.Integrations.*;


/**
 * the idea here is that we want a way to take any asset - be it a {@literal .wav} or a
 * {@literal .png} - and turn it into a {@literal  .mp3} and a 1MB-or-less
 * {@literal .jpg}.
 */
@Configuration
class MediaNormalizationIntegration {

	@Bean(AUDIO_NORMALIZATION_FLOW)
	IntegrationFlow audioNormalizationFlow() {
		return flow -> flow.handle(Integrations.debugHandler("audio flow"));
	}

	@Bean(IMAGE_NORMALIZATION_FLOW)
	IntegrationFlow imageNormalizationFlow(@Qualifier(ImageEncoder.IMAGE_ENCODER) MediaEncoder imageEncoder) {
		return flow -> flow.handle(Integrations.debugHandler("image flow")).transform(File.class, imageEncoder::encode);
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

abstract class EncoderUtils {

	static File tmp(File in) {
		Assert.notNull(in, "the file must not be null");
		var path = in.getAbsolutePath();
		var ext = path.substring(path.lastIndexOf("."));
		return new File(in.getParentFile(), UUID.randomUUID() + ext);
	}

}

@Component(ImageEncoder.IMAGE_ENCODER)
class ImageEncoder implements MediaEncoder {

	static final String IMAGE_ENCODER = "image-encoder";

	private final static Logger log = LoggerFactory.getLogger(ImageEncoder.class);

	public final static DataSize MAX_SIZE = DataSize.ofMegabytes(2);

	ImageEncoder() throws Exception {
		for (var tool : Set.of("convert", "identify"))
			try (var cmd = new ProcessBuilder().command(tool, "--version").start().getInputStream();
					var ow = new InputStreamReader(cmd)) {
				var cmdOutput = FileCopyUtils.copyToString(ow);
				log.debug(cmdOutput);
			}

	}

	@Override
	public File encode(File in) {
		try {
			var output = isValidImage(in)
					? Files.copy(in.toPath(), new File(in.getParentFile(), "copy.jpg").toPath()).toFile()
					: scale(convertFileToJpeg(in));

			log.debug("in: " + in.getAbsolutePath() + System.lineSeparator() + "out: " + output.getAbsolutePath()
					+ System.lineSeparator());

			return output;
		} //
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	private Dimension readDimensionsFor(File file) throws Exception {
		Assert.state(file.exists(), "the file [" + file.getAbsolutePath() + "] must exist");
		var process = new ProcessBuilder().command("identify", "-ping", "-format", "%w %h", file.getAbsolutePath())
			.start();
		var finished = process.waitFor();
		Assert.state(finished == 0, "the process should exit normally.");
		try (var in = new InputStreamReader(process.getInputStream())) {
			var output = FileCopyUtils.copyToString(in).strip();
			log.debug(output);
			var parts = output.split(" ");
			Assert.isTrue(parts.length == 2,
					"there should be two " + "parts to the output for the image " + file.getAbsolutePath());
			var w = Integer.parseInt(parts[0].strip());
			var h = Integer.parseInt(parts[1].strip());
			return new Dimension(w, h);
		}
	}

	private boolean isValidSize(File in) {
		return (in.length() < MAX_SIZE.toBytes());
	}

	private File convertFileToJpeg(File in) throws Exception {
		if (isValidType(in))
			return in;
		var converted = EncoderUtils.tmp(in);
		var convert = new ProcessBuilder().command("convert", in.getAbsolutePath(), converted.getAbsolutePath())
			.start();
		Assert.state(convert.waitFor() == 0, "the process should exit normally");
		return converted;
	}

	private boolean isValidType(File in) {
		return in.getName().toLowerCase(Locale.ROOT).endsWith(".jpg");
	}

	private File scale(File file, double width) throws Exception {
		var tmp = EncoderUtils.tmp(file);
		var cmd = new String[] { "convert", file.getAbsolutePath(), "-resize", "100x" + ((int) width) + "^", "-gravity",
				"center", tmp.getAbsolutePath() };
		var process = new ProcessBuilder().command(cmd).start();

		var result = process.waitFor();

		if (log.isDebugEnabled()) {
			try (var err = new InputStreamReader(process.getErrorStream());
					var out = new InputStreamReader(process.getInputStream())) {
				log.debug("error: " + FileCopyUtils.copyToString(err));
				log.debug("out: " + FileCopyUtils.copyToString(out));
			}
		}
		Assert.state(result == 0, "the conversion must exit successfully");
		return tmp;
	}

	private File scale(File file) throws Exception {
		var decrement = 500;
		while (!isValidSize(file)) {
			file = scale(file, readDimensionsFor(file).getWidth() - decrement);
			var width = readDimensionsFor(file).getWidth();
			Assert.state(width >= 100, "the width must be 100px or more");
		}
		return file;
	}

	private boolean isValidImage(File f) {
		return isValidSize(f) && isValidType(f);
	}

}
