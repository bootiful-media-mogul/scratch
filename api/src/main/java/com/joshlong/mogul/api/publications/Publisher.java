package com.joshlong.mogul.api.publications;

import com.joshlong.mogul.api.*;
import com.joshlong.mogul.api.blogs.Blog;
import com.joshlong.mogul.api.podcasts.Podcast;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

interface PublicationService {
    <T> void publish(Long mogulId, String payload, Map<String, String> context, PublisherPlugin<T> plugin);
}

@Service
@Transactional
class DefaultPublicationService
        implements PublicationService {

    private final JdbcClient db;
    private final Settings settings;
    private final MogulService mogulService;
    private final Map<String, PublisherPlugin<?>> plugins;

    DefaultPublicationService(JdbcClient db, Settings settings, MogulService mogulService, Map<String, PublisherPlugin<?>> plugins) {
        this.db = db;
        this.settings = settings;
        this.mogulService = mogulService;
        this.plugins = plugins;
        Assert.notNull(this.db, "the JdbcClient must not be null");
        Assert.notNull(this.mogulService, "the mogulService must not be null");
        Assert.notNull(this.settings, "the settings must not be null");
        Assert.state(!this.plugins.isEmpty(), "there are no plugins for publication");
    }


    @Override
    public <T> void publish(Long mogulId, String payload, Map<String, String> contextAndSettings,
                            PublisherPlugin<T> plugin) {
        Assert.notNull(plugin, "the plugin must not be null");
        Assert.notNull(payload, "your payload must not be null");
        var mogul = this.mogulService.getMogulById(mogulId);
        Assert.notNull(mogul, "the mogul should not be null");

        var settings = this.settings.getByCategory(mogulId, plugin.name());

        var finalMapOfConfig = new HashMap<String, String>();
        for (var c : contextAndSettings.keySet())
            finalMapOfConfig.put(c, contextAndSettings.get(c));
        for (var c : settings.keySet())
            finalMapOfConfig.put(c, settings.get(c).value());

        var publication = Publication.of(mogul, plugin.name() , finalMapOfConfig, payload);

        // todo write this publication to the db
        // and in a separate thread have spring integration pull down
        // the publications that havent completed yet and try to fulfill them, making sure to serialize
        // the configuration using Jackson
        // and then encrypting the serialized blob!
        // the spring integration thread should pull down the work, lock the record (need a new flag), and then
        // call plugin.publish. the plugin itself could use the spring integration pattern to hide a whole series of
        // steps like producing audio, reducing thumbnail size, and then uplaoding the files to podbean itself.
        // once its published, once `publish` returns it should return information in a PublicationStatus (Map<String,String>)
        //
        // wwe need to decouple produced audio from the publishing event.
        // so lets say every time we update a podcast, we save a dirty bit to the db saying the produced audio for this file needs updatiung)
        // t hen we publish an epplication event  that then kicks off a to two places at once: one to produce a minimal thumbnail
        // and another to produce an audio file. once both of those finisxh, theyll publish an event that then updates the row further
        // with the appropriate produced_audio_file and produced_thumbnail_file and maybe one day the produced transcripts
        // it should be that when you save new photos and audio, it nulls out the relevant fields in the podcast table and deletes the underlying managed file,s
        // in effect, graying out the `publish` button.
        // maybe each client could have a server sent event stream telling it when a podcast has been produced in the background, and thus
        // ungreying the publish button for folks?
        plugin.publish(publication);
    }
}

@Configuration
class PublisherPluginConfiguration {

    @Bean
    ApplicationRunner demo(
            PodcastService podcastService,
            PublicationService publicationService,
            MogulSecurityContexts mogulSecurityContexts,
            @Qualifier("podbean") PublisherPlugin<Podcast> podbeanPlugin,
            MogulService mogulService) {
        return args -> {
            var mogul = mogulService.getMogulByName("jlong");
            var auth = mogulSecurityContexts.install(mogul.id());
            System.out.println("got the following authentication: [" + auth + "]");
            var podcast = podcastService.getAllPodcastsByMogul(mogul.id()).iterator().next();
            publicationService.publish(mogul.id(), Long.toString(podcast.id()), new HashMap<>(), podbeanPlugin);
        };
    }
}


interface PublisherPlugin<T> {

    String name();

    boolean supports(Publication publication);

    void publish(Publication publication);

    PublicationStatus<T> status(Publication publication);
}

record PublicationStatus<T>(Publication publication) {
}

interface BlogPublisherPlugin extends PublisherPlugin<Blog> {
}

interface PodcastPublisherPlugin extends PublisherPlugin<Podcast> {
}


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
    public PublicationStatus<Podcast> status(Publication publication) {
        return null;
    }


}

@Component
class GithubBlogPlugin implements PublisherPlugin<Blog>, BeanNameAware {

    private final AtomicReference<String> beanName = new AtomicReference<>();

    @Override
    public String name() {
        return this.beanName.get();
    }

    @Override
    public void setBeanName(@NotNull String name) {
        this.beanName.set(name);
    }

    @Override
    public boolean supports(Publication publication) {
        return false;
    }

    @Override
    public void publish(Publication publication) {

    }

    @Override
    public PublicationStatus<Blog> status(Publication publication) {
        return null;
    }
}


class Publication {
    private final Mogul mogul;
    private final Date created;
    private final Date published;
    private final String payload;
    private final String plugin;
    private final Map<String, String> context;

    Publication(Mogul mogul, String plugin, Date created, Date published, Map<String, String> context, String payload) {
        this.mogul = mogul;
        this.context = context;
        this.plugin = plugin;
        this.created = created;
        this.published = published;
        this.payload = payload;
    }

    Publication(Mogul mogul, String plugin, Date created, String payload) {
        this(mogul, plugin, created, null, new HashMap<>(), payload);
    }

    Publication(Mogul mogul, String plugin, Date created, Map<String, String> context, String payload) {
        this(mogul, plugin, created, null, context, payload);
    }

    public Mogul mogul() {
        return this.mogul;
    }

    public Date created() {
        return this.created;
    }

    public Date published() {
        return this.published;
    }

    public String payload() {
        return this.payload;
    }

    public Map<String, String> context() {
        return this.context;
    }

    public String plugin() {
        return plugin;
    }

    public static Publication of(Mogul mogul, String plugin , Map<String, String> context, String payload) {
        return new Publication(mogul,  plugin , new Date(), context, payload);
    }
}
