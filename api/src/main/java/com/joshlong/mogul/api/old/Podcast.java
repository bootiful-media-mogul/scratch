package com.joshlong.mogul.api.old;

import java.net.URI;
import java.util.Date;

@Deprecated
public record Podcast(Long mogulId, Long id, String uid, Date date, String description, String transcript, String title,
		Podbean podbean, String notes, S3 s3) {

	public record Podbean(String id, URI photo, URI media, URI player, URI permalink) {
	}

	public record S3(Audio audio, Photo photo) {
		public record Audio(URI uri, String fileName) {
		}

		public record Photo(URI uri, String fileName) {
		}
	}

}
