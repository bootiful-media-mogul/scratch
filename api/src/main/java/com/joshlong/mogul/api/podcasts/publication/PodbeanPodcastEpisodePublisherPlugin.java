package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.podcasts.production.PodcastProducer;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * acts as the interface from the client into the podbean pipeline
 */
@Component(PodbeanPodcastEpisodePublisherPlugin.PLUGIN_NAME)
class PodbeanPodcastEpisodePublisherPlugin implements PodcastEpisodePublisherPlugin, BeanNameAware {


	public static final String PLUGIN_NAME = "podbean";

	private final PodcastProducer podcastProducer;

	private final AtomicReference<String> beanName = new AtomicReference<>();

	PodbeanPodcastEpisodePublisherPlugin(PodcastProducer podcastProducer) {
		this.podcastProducer = podcastProducer;
	}

	@Override
	public String name() {
		return this.beanName.get();
	}

	@Override
	public void setBeanName(@NonNull String name) {
		this.beanName.set(name);
	}

	@Override
	public boolean supports(Map<String, String> context, Episode payload) {
		return context.containsKey("clientId") && context.containsKey("clientSecret");
	}

	@Override
	public void publish(Map<String, String> context, Episode payload) {
		System.out.println("publishing to podbean with context [" + context + "] and payload [" + payload + "]");
		var producedAudioManagedFile = this.podcastProducer.produce(payload);
		System.out.println("produced audio, it is [" + producedAudioManagedFile + "]");
		// todo send to podbean

	}

	@Override
	public void unpublish(Map<String, String> context, Episode payload) {
		System.out.println("unpublishing to podbean with context [" + context + "] and payload [" + payload + "]");
	}

}
