package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.podcasts.Podcast;
import com.joshlong.mogul.api.podcasts.Segment;

import java.util.Collection;
import java.util.List;

public interface PodcastService {

	String PODCAST_EPISODES_BUCKET = "mogul-podcast-episodes";



	Segment createEpisodeSegment(
			Long mogulId, Long episodeId,
			String name, long crossfade, int order
	);

  void movePodcastEpisodeSegmentUp(Long episode, Long segment)  ;
  void movePodcastEpisodeSegmentDown(Long episode, Long segment)  ;

	void deletePodcastEpisodeSegment(Long episodeSegmentId);

	void updateEpisodeSegmentOrder(Long episodeSegmentId, int order);

	Segment getEpisodeSegmentById(Long episodeSegmentId);

	List<Segment> getEpisodeSegmentsByEpisode(Long id);

	Collection<Podcast> getAllPodcastsByMogul(Long mogulId);

	Collection<Episode> getEpisodesByPodcast(Long podcastId);

	Podcast createPodcast(Long mogulId, String title);

	Episode createPodcastEpisode(Long podcastId, String title, String description, ManagedFile graphic,
								 ManagedFile producedGraphic, ManagedFile producedAudio);

	Podcast getPodcastById(Long podcastId);

	Episode getEpisodeById(Long episodeId);

	void deletePodcast(Long podcastId);

	void deletePodcastEpisode(Long episodeId);

	Episode createPodcastEpisodeDraft(Long currentMogulId, Long podcastId, String title, String description);

	Episode updatePodcastEpisodeDraft(Long episodeId, String title, String description);

	Episode writePodcastEpisodeProducedAudio(Long episodeId, Long managedFileId);

}
