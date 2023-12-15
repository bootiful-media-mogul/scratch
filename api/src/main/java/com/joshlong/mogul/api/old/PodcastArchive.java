package com.joshlong.mogul.api.old;

import org.springframework.core.io.Resource;

public record PodcastArchive(String uuid, String title, String description, Resource introduction, Resource interview,
		Resource image) {

}
