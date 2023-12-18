package com.joshlong.mogul.api.managedfiles;



import java.util.Date;

/**
 * represents a persistent, managed file stored on cloud storage
 */
public record ManagedFile(Long mogulId, Long id, String bucket,
						  /* the idea is that the name in S3 might be different than the name the user gave us. */
						  String storageFilename, String folder, String filename, Date created,
		boolean written, long size ,  String contentType) {
}
