package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.UUID;

@Controller
class PodcastController {

	private final MogulService mogulService;

	private final PodcastService podcastService;

	private final ManagedFileService managedFileService;

	PodcastController(MogulService mogulService, PodcastService podcastService, ManagedFileService managedFileService) {
		this.mogulService = mogulService;
		this.podcastService = podcastService;
		this.managedFileService = managedFileService;
	}

	@QueryMapping
	Podcast podcastById(@Argument Long id) {
		return this.podcastService.getPodcastById(id);
	}

	@QueryMapping
	Collection<Podcast> podcasts() {
		return this.podcastService.getAllPodcastsByMogul(mogulService.getCurrentMogul().id());
	}

	@SchemaMapping(typeName = "Podcast")
	Collection<Episode> episodes(Podcast podcast) {
		this.mogulService.assertAuthorizedMogul(podcast.mogulId());
		return this.podcastService.getEpisodesByPodcast(podcast.id());
	}

	@MutationMapping
	Long deletePodcast(@Argument Long id) {
		var podcast = this.podcastService.getPodcastById(id);
		Assert.notNull(podcast, "the podcast is null");
		var mogulId = podcast.mogulId();
		this.mogulService.assertAuthorizedMogul(mogulId);
		var podcasts = this.podcastService.getAllPodcastsByMogul(mogulId);
		Assert.state(!podcasts.isEmpty() && podcasts.size() - 1 > 0,
				"you must have at least one active, non-deleted podcast");
		this.podcastService.deletePodcast(podcast.id());
		return id;
	}

	@MutationMapping
	Podcast createPodcast(@Argument String title) {
		Assert.hasText(title, "the title for the podcast must be non-empty!");
		return podcastService.createPodcast(mogulService.getCurrentMogul().id(), title);
	}

	@MutationMapping
	Episode createPodcastEpisodeDraft(@Argument Long podcastId, @Argument String title, @Argument String description) {
		var currentMogulId = mogulService.getCurrentMogul().id();
		var podcast = podcastService.getPodcastById(podcastId);
		Assert.notNull(podcast, "the podcast is null!");
		mogulService.assertAuthorizedMogul(podcast.mogulId());
		return podcastService.createPodcastEpisodeDraft(currentMogulId, podcastId, title, description);

	}

}
