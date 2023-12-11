package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.podcasts.Podcast;

import java.util.Collection;

public interface PodcastService {

    String PODCAST_EPISODES_BUCKET = "podcast-episodes";

    Collection<Podcast> getAllPodcastsByMogul(Long mogulId);

    Collection <Episode> getEpisodesByPodcast(Long podcastId) ;

    //    PodbeanAccountSettings getPodbeanAccountSettings(Long mogulId);
//
//    PodbeanAccountSettings configurePodbeanAccountSettings(Long mogulId, String clientId, String clientSecret);
    Podcast createPodcast(Long mogulId, String title);

    Episode createPodcastEpisode(Long podcastId,
                                 String title,
                                 String description,
                                 ManagedFile graphic,
                                 ManagedFile introduction,
                                 ManagedFile interview
    );

    Podcast getPodcastById(Long podcastId);

    Episode getEpisodeById(Long episodeId) ;

    void deletePodcast(Long podcastId);
}
