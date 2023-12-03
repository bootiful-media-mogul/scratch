package com.joshlong.mogul.api.podcasts;

import java.net.URI;
import java.util.Date;

record Podcast(Integer id, String uid, Date date, String description, String transcript, String title, Podbean podbean,
		String notes, S3 s3 ) {

	record Podbean(String id, Date draftCreated, Date draftPublished, URI photo, URI media) {
	}

	record S3(Audio audio, Photo photo) {

		record Audio(URI uri, String fileName) {
		}

		record Photo(URI uri, String fileName) {
		}
	}

}
