package com.joshlong.mogul.api.old;

import java.util.Collection;
@Deprecated
record SearchIndexUpdatedEvent(Collection<PodcastView> podcastViews) {
}
