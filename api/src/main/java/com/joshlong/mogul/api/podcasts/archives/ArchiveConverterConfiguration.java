package com.joshlong.mogul.api.podcasts.archives;

import com.joshlong.mogul.api.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.io.File;

@Configuration
class ArchiveConverterConfiguration {

	private static Logger log = LoggerFactory.getLogger(ArchiveConverterConfiguration.class);

	/*
	 *
	 * // use this to write a new archive with the sample assets. this is a slow thing //
	 * to do so only do it if you're sure ApplicationRunner
	 * sampleArchiveDebugRunner(ArchiveConverter archiveConverter, ApiProperties
	 * properties) { return args -> { if (properties.debug()) { var archives =
	 * properties.podcasts().pipeline().archives(); var sampleArchive = new
	 * File(SystemPropertyUtils .resolvePlaceholders(
	 * "${HOME}/Desktop/misc/bootiful-podcast-v3/sample-archive/archive"));
	 * Assert.state(sampleArchive.exists(), "the archive must exist"); var uuid =
	 * UUID.randomUUID().toString(); var pa = new PodcastArchive(uuid,
	 * "the title for UUID (" + uuid + ")",
	 * "this is a sample podcast that i created for UUID (" + uuid + ")", new
	 * FileSystemResource(new File(sampleArchive, "intro.mp3")), new
	 * FileSystemResource(new File(sampleArchive, "interview.mp3")), new
	 * FileSystemResource(new File(sampleArchive, "image.jpg"))); var file = new
	 * File(archives, uuid + ".tmp"); try (var out = new FileOutputStream(file)) {
	 * archiveConverter.serialize(pa, out); Assert.state(file.exists() && file.length() >
	 * 0, "the archive should exist"); log.debug("wrote the archive to " +
	 * file.getAbsolutePath() + '.'); } Files.move(file.toPath(), new
	 * File(file.getParentFile(), file.getName() + ".zip").toPath()); } }; }
	 */

	@Bean
	ArchiveConverter archiveConverter(ApiProperties properties) {
		var extraction = new File(properties.podcasts().pipeline().root(), "extraction");
		Assert.state(extraction.exists() || extraction.mkdirs(), "the extraction directory does not exist");
		return new ArchiveConverter(extraction);
	}

}
