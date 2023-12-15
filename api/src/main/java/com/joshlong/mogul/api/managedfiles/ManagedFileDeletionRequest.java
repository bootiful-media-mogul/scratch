package com.joshlong.mogul.api.managedfiles;

import java.util.Date;
public  record ManagedFileDeletionRequest( Long id, Long mogulId, String bucket, String folder, String filename, boolean deleted,
                                  Date created) {
}
