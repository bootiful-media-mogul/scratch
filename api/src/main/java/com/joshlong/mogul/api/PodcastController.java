package com.joshlong.mogul.api;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

@Controller
@ResponseBody
class PodcastController {

	private final MogulService service;

	private final DateTimeFormatter formatter;

	PodcastController(MogulService service, DateTimeFormatter formatter) {
		this.service = service;
		this.formatter = formatter;
	}

	@QueryMapping
	Collection<Map<String, Object>> podcasts() {
		var name = SecurityContextHolder.getContext().getAuthentication().getName();
		var mogul = this.service.getMogulByName(name);
		var podcasts = this.service.getPodcastsByMogul(mogul.id());
		return podcasts.stream()
			.map(podcast -> Map.of("date", this.formatter.format(podcast.date().toInstant()), "title", podcast.title(),
					"description", (Object) podcast.description()))
			.toList();
	}

}
