package com.joshlong.mogul.api.managedfiles;

import java.util.Date;

/**
 * represents a persistent, managed file stored on cloud storage
 */
public record ManagedFile(Long mogulId, Long id, String bucket, String folder, String filename, Date created,
		boolean written, long size) {
}
