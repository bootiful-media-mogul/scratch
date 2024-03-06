package com.joshlong.mogul.api.settings;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Settings;
import com.joshlong.mogul.api.publications.PublisherPlugin;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * the idea is that this endpoint should handle receiving updates to and displaying the validity of all
 * the various configuration values required by the system for each given user.
 */
@ResponseBody
@Controller
class SettingsController {

    private final Map<String, PublisherPlugin<?>> plugins = new ConcurrentHashMap<>();

    private final MogulService mogulService;
    private final Settings settings;

    SettingsController(Map<String, PublisherPlugin<?>> ps, MogulService mogulService, Settings settings) {
        this.mogulService = mogulService;

        this.settings = settings;
        this.plugins.putAll(ps); // copy the values

    }

    record Setting(String name, boolean valid) {
    }

    record SettingsPage(boolean valid, String category, List<Setting> settings) {
    }

    record SettingsPages(List<SettingsPage> settings) {
    }


    @GetMapping("/settings-pages")
    SettingsPages settings() {


        var currentMogulId = this.mogulService.getCurrentMogul().id();
        var pages = new ArrayList<SettingsPage>();

        var orderedPluginNames = new ArrayList<String>();
        orderedPluginNames.addAll(this.plugins.keySet());
        orderedPluginNames.sort(Comparator.naturalOrder());

        for (var pluginName : orderedPluginNames) {
            var plugin = plugins.get(pluginName);
            var pageIsValid = plugin.isConfigurationValid(this.settings.getAllValuesByCategory(currentMogulId, pluginName));
            var page = new SettingsPage(pageIsValid, pluginName, new ArrayList<>());
            pages.add(page);

            var requiredKeys = new ArrayList<>(plugin.getRequiredSettingKeys());
            requiredKeys.sort(Comparator.naturalOrder());
            for (var k : requiredKeys) {
                var valid = StringUtils.hasText(settings.getValue(currentMogulId, pluginName, k));
                page.settings().add(new Setting(k, valid));
            }


        }


        var sp = new SettingsPages(pages);

        System.out.println("settingsPages: " + sp);

        return sp;

    }
}
