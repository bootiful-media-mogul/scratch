package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.podbean.Episode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;

@Configuration
class PodcastPromotionIntegrationFlowConfiguration {

}
