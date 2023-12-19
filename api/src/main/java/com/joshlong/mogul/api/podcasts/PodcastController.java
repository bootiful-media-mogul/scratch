package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.Settings;
import com.joshlong.mogul.api.podcasts.publication.PodcastEpisodePublisherPlugin;
import com.joshlong.mogul.api.publications.PublicationService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

@Controller
class PodcastController {

	private final MogulService mogulService;

	private final PodcastService podcastService;

	private final Map<String, PodcastEpisodePublisherPlugin> plugins;

	private final PublicationService publicationService;

	private final Settings settings;

	@QueryMapping
	Collection<Episode> podcastEpisodesByPodcast(@Argument Long podcastId) {
		return this.podcastService.getEpisodesByPodcast(podcastId);
	}

	@MutationMapping
	Episode updatePodcastEpisode(@Argument Long episodeId, @Argument String title, @Argument String description) {
		return this.podcastService.updatePodcastEpisodeDraft(episodeId, title, description);
	}

	@QueryMapping
	Episode podcastEpisodeById (@Argument Long id ) {
		return this.podcastService.getEpisodeById(id);
	}

	PodcastController(MogulService mogulService, PodcastService podcastService, Map<String, PodcastEpisodePublisherPlugin> plugins, PublicationService publicationService, Settings settings) {
		this.mogulService = mogulService;
		this.podcastService = podcastService;
		this.plugins = plugins;
		this.publicationService = publicationService;
		this.settings = settings;
	}


	@SchemaMapping
	Collection<String> availablePlugins(Episode episode) {
		var mogul = mogulService.getCurrentMogul();
		var plugins = new HashSet<String>();
		for (var pluginName : this.plugins.keySet()) {
			var configuration = settings.getAllValuesByCategory(mogul.id(), pluginName);
			var plugin = this.plugins.get(pluginName);
			if (plugin.supports(configuration, episode)) {
				plugins.add(plugin.name());
			}
		}
		return plugins;
	}


	@SchemaMapping
	long created(Episode episode) {
		return episode.created().getTime();
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
	Long deletePodcastEpisode(@Argument Long id) {
		var ep = podcastService.getEpisodeById(id);
		this.mogulService.assertAuthorizedMogul(ep.podcast().mogulId());
		podcastService.deletePodcastEpisode(id);
		return id;
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
	boolean publishPodcastEpisode(@Argument Long episodeId, @Argument String pluginName) {
		var episode = this.podcastService.getEpisodeById(episodeId);
		Assert.notNull(episode, "the episode should not be null");
		Assert.state(this.plugins.containsKey(pluginName), "the plugin [" + pluginName + "] does not exist or is not applicable for [" + episode + "]");
		var plugin = this.plugins.get(pluginName);
		var configuration = this.settings.getAllValuesByCategory(this.mogulService.getCurrentMogul().id(), pluginName);
		plugin.publish(configuration, episode);
		return false;

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
