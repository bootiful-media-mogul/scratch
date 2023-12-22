package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.publications.Publishable;

import java.io.Serializable;
import java.util.Date;

public record Episode(Long id, Podcast podcast, String title, String description, Date created, ManagedFile graphic,
		ManagedFile producedGraphic, ManagedFile introduction, ManagedFile producedIntroduction, ManagedFile interview,
		ManagedFile producedInterview, ManagedFile producedAudio, boolean complete, Date producedAudioUpdated,
		Date producedAudioAssetsUpdated) implements Publishable {
	@Override
	public Serializable publicationKey() {
		return this.id();
	}
}
