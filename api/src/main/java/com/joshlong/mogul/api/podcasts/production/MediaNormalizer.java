package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface MediaNormalizer {

	@Gateway(requestChannel = MediaNormalizationIntegration.REQUESTS)
	ManagedFile normalize(ManagedFile input);

}
