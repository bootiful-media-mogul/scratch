package com.joshlong.mogul.api;

import java.net.URI;
import java.util.Date;

public record Podcast(Long mogulId, Long id, String uid, Date date, String description, String transcript, String title,
		Podbean podbean, String notes, S3 s3) {

	public record Podbean(String id, Date draftCreated, Date draftPublished, URI photo, URI media) {
	}

	public record S3(Audio audio, Photo photo) {
		public record Audio(URI uri, String fileName) {
		}

		public record Photo(URI uri, String fileName) {
		}
	}

}
