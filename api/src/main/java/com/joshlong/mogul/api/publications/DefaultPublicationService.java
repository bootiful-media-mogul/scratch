package com.joshlong.mogul.api.publications;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Settings;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

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
                            PublisherPlugin  plugin) {
        Assert.notNull(plugin, "the plugin must not be null");
        Assert.notNull(payload, "your payload must not be null");
       // var publication = createPublication(mogulId, payload, contextAndSettings, plugin);

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
        // plugin.publish(publication);
    }

    @NotNull
    private Publication createPublication(Long mogulId, String payload, Map<String, String> contextAndSettings, PublisherPlugin plugin) {
        var mogul = this.mogulService.getMogulById(mogulId);
        Assert.notNull(mogul, "the mogul should not be null");

        var settings = this.settings.getAllSettingsByCategory(mogulId, plugin.name());

        var finalMapOfConfig = new HashMap<String, String>();
        for (var c : contextAndSettings.keySet())
            finalMapOfConfig.put(c, contextAndSettings.get(c));
        for (var c : settings.keySet())
            finalMapOfConfig.put(c, settings.get(c).value());

        return Publication.of(mogul, plugin.name(), finalMapOfConfig, payload);
    }
}
