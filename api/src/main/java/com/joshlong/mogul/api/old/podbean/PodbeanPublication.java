package com.joshlong.mogul.api.old.podbean;

import java.util.Date;

public record PodbeanPublication(Long mogulId, Long podcastId, String nodeId, boolean continueTracking, Date started,
		Date stopped) {
}
