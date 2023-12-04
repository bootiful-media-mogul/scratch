package com.joshlong.mogul.api;

import java.util.List;

public interface MogulService {

	Mogul getMogulById(Long id);

	Mogul getMogulByName(String name);

	PodbeanAccount getPodbeanAccountByMogul(Long mogulId);

	PodbeanAccount configurePodbeanAccount(Long mogulId, String clientId, String clientSecret);

	List<Podcast> getPodcastsByMogul(Long mogul);

	/** use to return <em>all</em> podcast episodes, regardless of {@code mogulId} */
	List<Podcast> getAllPodcasts();

	Podcast addPodcastEpisode(Long mogulId, Podcast podcast);

	Podcast getPodcastByPodbeanEpisode(String podbeanEpisodeId);

	Podcast markPodcastForPromotion(Podcast podcast);

}
