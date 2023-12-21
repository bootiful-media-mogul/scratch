package com.joshlong.mogul.api.publications;

import java.util.Map;

public interface PublisherPlugin<T> {

	String name();

	boolean supports(Map<String, String> context, T payload);

	void publish(Map<String, String> context, T payload);

	void unpublish(Map<String, String> context, T payload);

}
