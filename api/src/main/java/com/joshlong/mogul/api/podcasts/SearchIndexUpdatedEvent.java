package com.joshlong.mogul.api.podcasts;

import java.util.Collection;

record SearchIndexUpdatedEvent(Collection<PodcastView> podcastViews) {
}
