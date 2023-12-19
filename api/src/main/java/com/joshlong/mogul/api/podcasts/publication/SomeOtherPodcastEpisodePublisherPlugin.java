package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.podcasts.Episode;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component("demo")
class SomeOtherPodcastEpisodePublisherPlugin implements PodcastEpisodePublisherPlugin, BeanNameAware {

	private final AtomicReference<String> beanName = new AtomicReference<>();

	@Override
	public void setBeanName(String name) {
		this.beanName.set(name);
	}

	@Override
	public String name() {
		return this.beanName.get();
	}

	@Override
	public boolean supports(Map<String, String> context, Episode payload) {
		return true;
	}

	@Override
	public void publish(Map<String, String> context, Episode payload) {

		System.out.println(
				"publishing [" + payload.toString() + "] with [" + beanName.get() + "] and context [" + context + "] ");

	}

	@Override
	public void unpublish(Map<String, String> context, Episode payload) {

	}

}
