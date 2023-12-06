package com.joshlong.mogul.api;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface MogulService {

	Mogul getCurrentMogul();

	Mogul login(Authentication principal);

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

	PodcastDraft createPodcastDraft( Long mogulId,String uuid);
	PodcastDraft getPodcastDraftByUid( String uuid);
}
