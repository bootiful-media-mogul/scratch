package com.joshlong.mogul.api;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import org.springframework.core.io.Resource;

public interface  ManagedFileService {

    ManagedFile getManagedFile (Long managedFileId) ;

    Resource read (Long managedFileId) ;

    ManagedFile write  (Long managedFileId, Resource resource) ;

    ManagedFile createManagedFile (Long mogulId,
                                   String bucket, String folder, String fileName,
                                   long size) ;
}
