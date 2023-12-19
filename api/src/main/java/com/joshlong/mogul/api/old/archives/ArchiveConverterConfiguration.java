package com.joshlong.mogul.api.old.archives;

import com.joshlong.mogul.api.ApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import java.io.File;

//@Configuration
class ArchiveConverterConfiguration {

	private static Logger log = LoggerFactory.getLogger(ArchiveConverterConfiguration.class);

	@Bean
	ArchiveConverter archiveConverter(ApiProperties properties) {
		var extraction = new File(properties.podcasts().pipeline().root(), "extraction");
		Assert.state(extraction.exists() || extraction.mkdirs(), "the extraction directory does not exist");
		return new ArchiveConverter(extraction);
	}

}
