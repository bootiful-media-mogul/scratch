package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.podcasts.Podcast;
import com.joshlong.mogul.api.publications.Publication;
import com.joshlong.mogul.api.publications.PublicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicReference;

@Component("podbean")
class PodbeanPodcastPlugin implements PodcastPublisherPlugin, BeanNameAware {

    private final AtomicReference<String> beanName = new AtomicReference<>();
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PodcastService podcastService;

    PodbeanPodcastPlugin(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    @Override
    public String name() {
        return this.beanName.get();
    }

    @Override
    public void setBeanName(@NonNull String name) {
        this.beanName.set(name);
    }

    Podcast from(String payload) {
        return this.podcastService.getPodcastById(Long.parseLong(payload));
    }

    @Override
    public boolean supports(Publication publication) {
        var payload = publication.payload();
        try {
            Assert.notNull(from(payload), "could not find the podcast with id [" + payload + "] ");
            return true;
        } //
        catch (Throwable throwable) {
            log.error("got an exception [" + throwable + "]");
        }
        return false;
    }

    @Override
    public void publish(Publication publication) {
        System.out.println("publishing [" + publication + "]");
    }

    @Override
    public PublicationStatus status(Publication publication) {
        return null;
    }


}
