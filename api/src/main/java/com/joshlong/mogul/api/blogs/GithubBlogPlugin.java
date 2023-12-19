package com.joshlong.mogul.api.blogs;

import com.joshlong.mogul.api.publications.PublisherPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
class GithubBlogPlugin implements PublisherPlugin<Blog>, BeanNameAware {

	private final AtomicReference<String> beanName = new AtomicReference<>();

	@Override
	public String name() {
		return this.beanName.get();
	}

	@Override
	public boolean supports(Map<String, String> context, Blog payload) {
		return context.containsKey("clientId") && context.containsKey("clientSecret");
	}

	@Override
	public void publish(Map<String, String> context, Blog payload) {
		System.out.println("publishing to github for payload [" + payload + "]");
	}

	@Override
	public void unpublish(Map<String, String> context, Blog payload) {
		System.out.println("unpublishing to github for payload [" + payload + "]");
	}

	@Override
	public void setBeanName(@NotNull String name) {
		this.beanName.set(name);
	}

}
