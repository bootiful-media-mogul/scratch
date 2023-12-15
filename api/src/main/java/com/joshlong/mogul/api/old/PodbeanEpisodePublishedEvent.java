package com.joshlong.mogul.api.old;

import com.joshlong.mogul.api.Podcast;
import com.joshlong.podbean.Episode;

public record PodbeanEpisodePublishedEvent(Podcast podcast, Episode episode) {
}
