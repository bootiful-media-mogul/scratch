package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.podcasts.Episode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component(PodbeanPodcastEpisodePublisherPlugin.PLUGIN_NAME)
class PodbeanPodcastEpisodePublisherPlugin implements PodcastEpisodePublisherPlugin, BeanNameAware {

	public static final String PLUGIN_NAME = "podbean";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicReference<String> beanName = new AtomicReference<>();

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
		log.debug("publishing to podbean with context [" + context + "] and payload [" + payload + "]. "
				+ "produced audio is [" + payload.producedAudio() + "]");
	}

	@Override
	public void unpublish(Map<String, String> context, Episode payload) {
		log.debug("unpublishing to podbean with context [" + context + "] and payload [" + payload + "]");
	}

}
