package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.MogulSecurityContexts;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.mogul.api.Storage;
import com.joshlong.podbean.EpisodeStatus;
import com.joshlong.podbean.EpisodeType;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;

@Configuration
class PodcastDeletionIntegrationFlowConfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Bean
    IntegrationFlow deletePodcastIntegrationFlow(
            MogulSecurityContexts mogulSecurityContexts,
            MogulService mogulService, Storage storage, PodbeanClient podbeanClient) {

        var messageSource = (MessageSource<Collection<Podcast>>)
                () -> MessageBuilder.withPayload(mogulService.getDeletedPodcasts()).build();

        return IntegrationFlow//
                .from(messageSource, pc -> pc.poller(pm -> PollerFactory.fixedRate(Duration.ofMinutes(1))))//
                .split(new AbstractMessageSplitter() {
                    @Override
                    protected Object splitMessage(Message<?> message) {
                        var payload = message.getPayload();
                        if (payload instanceof Collection<?> collection && !collection.isEmpty()) {
                            return collection;
                        }
                        return null;
                    }
                }) //
                .handle((GenericHandler<Podcast>) (payload, headers) -> {
                    var auth = mogulSecurityContexts.install(payload.id());
                    Assert.notNull(auth, "the resulting authentication is null");

                    podbeanClient.updateEpisode(payload.podbean().id(),
                            payload.title(), payload.description(), EpisodeStatus.DRAFT,
                            EpisodeType.PUBLIC, null, null);

                    log.info("hid the episode on Podbean itself for podcast [" + payload.id() + "] ");
                    var s3 = payload.s3();

                    var s3ToDelete = new HashSet<URI>();
                    if (s3 != null) {
                        var audio = s3.audio();
                        var photo = s3.photo();
                        if (audio != null && audio.uri() != null) s3ToDelete.add(audio.uri());
                        if (photo != null && photo.uri() != null) s3ToDelete.add(photo.uri());
                    }

                    for (var uri : s3ToDelete) {
                        var bucketName = uri.getHost();
                        var folderAndFile = uri.getPath();
                        log.info("going to s3 delete [" + bucketName + "] and [" + folderAndFile + "] for podcast [" + payload.id() + "] ");
                        storage.remove(bucketName, folderAndFile);
                    }
                    log.info("deleted the S3 assets themselves for podcast [" + payload.id() + "] ");
                    Assert.state(mogulService.deletePodcast(payload.id()), "could not delete the podcast [" + payload.id() + "]");
                    log.info("cleaned the DB state for podcast [" + payload.id() + "] ");
                    return null;
                })
                .get();
    }
}
