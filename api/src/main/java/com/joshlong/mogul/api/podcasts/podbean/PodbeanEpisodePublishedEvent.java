package com.joshlong.mogul.api.podcasts.podbean;

import fm.bootifulpodcast.podbean.Episode;

/**
 * a freshly published Podbean episode and its associated podcast
 *
 * @param podcastId the id of the podcast
 * @param episode the podbean episode
 * @author Josh Long
 */
public record PodbeanEpisodePublishedEvent(Long podcastId, Episode episode) {
}
