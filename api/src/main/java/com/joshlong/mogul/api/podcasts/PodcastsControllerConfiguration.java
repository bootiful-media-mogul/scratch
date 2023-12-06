package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.MogulService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Serializer;

@Configuration
class PodcastsControllerConfiguration {

	@Bean
	PodcastsController podcastsController(MogulService mogulService, ApiProperties properties,
			Serializer<PodcastArchive> podcastArchiveSerializer) {
		var pipeline = properties.podcasts().pipeline();
		return new PodcastsController(pipeline.drafts(), pipeline.archives(), mogulService, podcastArchiveSerializer);
	}

}
