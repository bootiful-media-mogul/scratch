package com.joshlong.mogul.api;

import com.joshlong.mogul.api.podcasts.podbean.PodbeanPublication;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;

import java.net.URI;
import java.util.Collection;
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

	Podcast connectPodcastToPodbeanPublication(Podcast podcast, String podbeanEpisodeId, URI logoUrl,
			URI podbeanPermalinkUrl, URI podbeanPlayerUrl);

	Podcast confirmPodbeanPublication(Podcast podcast, URI permalinkUrl, int duration);

	PodcastDraft createPodcastDraft(Long mogulId, String uuid);

	PodcastDraft getPodcastDraftByUid(String uuid);

	PodcastDraft completePodcastDraft(Long mogulId, String uid, String title, String description, Resource pictureFN,
			Resource introFN, Resource interviewFN);

	PodbeanAccountSettings configurePodbeanAccountSettings(Long mogulId, String clientId, String clientSecret);

	PodbeanAccountSettings getPodbeanAccountSettings(Long mogulId);

	// Collection<PodbeanPublication> getPodbeanPublicationsByNode(String nodeName);

	PodbeanPublication monitorPodbeanPublication(String nodeName, Podcast podcast);

	PodbeanPublication getPodbeanPublicationByPodcast(Podcast podcast);

	Collection<PodbeanPublication> getOutstandingPodbeanPublications();

	Podcast getPodcastById(Long podcastId);

	Collection<PodcastDraft> getPodcastDraftsByMogul(Long mogulId);

	Collection<Podcast> getDeletedPodcasts();

	boolean schedulePodcastForDeletion(Long podcastId);

	boolean deletePodcast(Long podcastId);

}
