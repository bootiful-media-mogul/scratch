package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.managedfiles.ManagedFile;

import java.util.Date;

public record Episode(Long id, Podcast podcast, String title, String description, Date created, ManagedFile graphic,
		ManagedFile introduction, ManagedFile interview, ManagedFile producedAudio) {
}
