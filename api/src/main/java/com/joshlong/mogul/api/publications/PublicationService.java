package com.joshlong.mogul.api.publications;

import java.util.Map;

public interface PublicationService {

	<T> void publish(Long mogulId, String payload, Map<String, String> context, PublisherPlugin plugin);

}
