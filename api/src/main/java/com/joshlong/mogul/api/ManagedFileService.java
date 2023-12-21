package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.managedfiles.ManagedFileDeletionRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.Collection;

public interface ManagedFileService {

	/**
	 * this will delete the record _and_ queue it up for deletion by a separate process in
	 * S3.
	 */

	Collection<ManagedFileDeletionRequest> getOutstandingManagedFileDeletionRequests();

	ManagedFileDeletionRequest getManagedFileDeletionRequest(Long managedFileDeletionRequestId);

	void completeManagedFileDeletion(Long managedFileDeletionRequestId);

	void deleteManagedFile(Long managedFileId);

	ManagedFile getManagedFile(Long managedFileId);

	Resource read(Long managedFileId);

	void write(Long managedFileId, String filename, MediaType mts, Resource resource);

	/**
	 * behind the scenes this variant simply creates a {@link FileSystemResource} which
	 * can be queried for its content length
	 */
	void write(Long managedFileId, String filename, MediaType mts, File resource);

	ManagedFile createManagedFile(Long mogulId, String bucket, String folder, String fileName, long size,
			MediaType mediaType);

}
