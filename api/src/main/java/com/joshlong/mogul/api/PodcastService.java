package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.podcasts.Podcast;

import java.util.Collection;

public interface PodcastService {

	String PODCAST_EPISODES_BUCKET = "mogul-podcast-episodes";

	Collection<Podcast> getAllPodcastsByMogul(Long mogulId);

	Collection<Episode> getEpisodesByPodcast(Long podcastId);

	Podcast createPodcast(Long mogulId, String title);

	Episode createPodcastEpisode(Long podcastId, String title, String description, ManagedFile graphic,
			ManagedFile introduction, ManagedFile interview, ManagedFile producedGraphic, ManagedFile producedIntro,
			ManagedFile producedInterview, ManagedFile producedAudio);

	Podcast getPodcastById(Long podcastId);

	Episode getEpisodeById(Long episodeId);

	void deletePodcast(Long podcastId);

	void deletePodcastEpisode(Long episodeId);

	Episode createPodcastEpisodeDraft(Long currentMogulId, Long podcastId, String title, String description);

	Episode updatePodcastEpisodeDraft(Long episodeId, String title, String description);

	Episode writePodcastEpisodeProducedAudio(Long episodeId, Long managedFileId);

}
