package com.joshlong.mogul.api.publications;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.podcasts.Podcast;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
class PublisherPluginConfiguration {
//
////    @Bean
//    ApplicationRunner demo(
//            PodcastService podcastService,
//            PublicationService publicationService,
//            MogulSecurityContexts mogulSecurityContexts,
//            @Qualifier("podbean") PublisherPlugin  podbeanPlugin,
//            MogulService mogulService) {
//        return args -> {
//            var mogul = mogulService.getMogulByName("jlong");
//            var auth = mogulSecurityContexts.install(mogul.id());
//            System.out.println("got the following authentication: [" + auth + "]");
//            var podcast = podcastService.getAllPodcastsByMogul(mogul.id()).iterator().next();
//            publicationService.publish(mogul.id(), Long.toString(podcast.id()), new HashMap<>(), podbeanPlugin);
//        };
//    }
}


