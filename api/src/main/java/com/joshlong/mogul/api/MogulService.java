package com.joshlong.mogul.api;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface MogulService {

	Mogul getCurrentMogul();

	Mogul login(Authentication principal);

	Mogul getMogulById(Long id);

	Mogul getMogulByName(String name);

	List<Podcast> getPodcastsByMogul(Long mogul);

	/** use to return <em>all</em> podcast episodes, regardless of {@code mogulId} */
	List<Podcast> getAllPodcasts();

	Podcast addPodcastEpisode(Long mogulId, Podcast podcast);

	Podcast getPodcastByPodbeanEpisode(String podbeanEpisodeId);

	Podcast markPodcastForPromotion(Podcast podcast);

	PodcastDraft createPodcastDraft(Long mogulId, String uuid);

	PodcastDraft getPodcastDraftByUid(String uuid);

	PodcastDraft completePodcastDraft(Long mogulId, String uid, String title, String description, Resource pictureFN,
			Resource introFN, Resource interviewFN);

	PodbeanAccountSettings configurePodbeanAccountSettings(Long mogulId, String clientId, String clientSecret);

	PodbeanAccountSettings getPodbeanAccountSettings(Long mogulId);

}
