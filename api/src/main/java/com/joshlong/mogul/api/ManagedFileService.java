package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.managedfiles.ManagedFileDeletionRequest;
import org.springframework.core.io.Resource;

import java.util.Collection;

public interface ManagedFileService {

	/**
	 * this will delete the record _and_ queue it up for deletion by a separate process in S3.
	 */

	Collection<ManagedFileDeletionRequest> getOutstandingManagedFileDeletionRequests();

	ManagedFileDeletionRequest getManagedFileDeletionRequest(Long managedFileDeletionRequestId);

	void complete(Long managedFileDeletionRequestId);

	void deleteManagedFile(Long managedFileId);

	ManagedFile getManagedFile(Long managedFileId);

	Resource read(Long managedFileId);

	ManagedFile write(Long managedFileId, String filename,  Resource resource);

	ManagedFile createManagedFile(Long mogulId, String bucket, String folder, String fileName, long size);

}

