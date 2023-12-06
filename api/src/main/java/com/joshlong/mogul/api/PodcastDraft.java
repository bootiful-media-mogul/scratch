package com.joshlong.mogul.api;

import java.util.Date;

/**
 * Represents a draft podcast used to stage, aggregate, and track the initial uploaded files
 */
public record PodcastDraft(
        Long id,
        boolean complete,
        String uid,
        Date date,
        String title,
        String description) {
}

