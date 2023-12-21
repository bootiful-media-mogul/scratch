package com.joshlong.mogul.api.managedfiles;

import java.net.URI;
import java.util.Date;

/**
 * represents a persistent, managed file stored on cloud storage
 */
public record ManagedFile(Long mogulId, Long id, String bucket, String storageFilename, String folder, String filename,
		Date created, boolean written, long size, String contentType) {

	public URI s3Uri() {
		return URI.create("s3://" + this.bucket() + "/" + this.folder() + '/' + this.storageFilename());
	}
}
