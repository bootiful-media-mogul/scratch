package com.joshlong.mogul.api.podcasts;

/**
 * im going to need to support a lot of destinations for publication of content (podcasts, blogs, etc.)
 * Right now we have settings for encrypted values
 * Could we have some sort of scheme where we have a property file blob (encrypted) in the DB (1-N) with a name that we
 * can then use to identify the bean that can handle the thing.
 * <p>
 * So we might have autoconfig that registers two different PodcastPublisher - `podbean` and `slef-hosted` or whatever.
 * In order for them to work, well need to specify credentials for each mogul tenant. So each tenant configures 0-N of these providers.
 * (Maybe each plugin exposes information on what each tenant should furnish credentials?) to make the plugin work? How? A JSON file?
 * We need to render a UI dynamically. Anyway, either way, once they’ve manipulated the UI to enter the required creds, we
 * can say that a given tenant has access to those plugins. We show the eligible plugins in a drop down in the ‘sync’ or
 * ‘publish’ stage of the pipeline. The user choose `podbean` and we load the property file , decrypt it, and then bind the right
 * object or the plugin.
 * <p>
 * Maybe theres a `T configure (Map<String, String> configPropsFromYamlEncryption) {}`
 * and each plugin has a multi tenant `T` proxy that is registered once but that will consult the mogul tenant and load
 * the requisite config and then work with its credentials
 * We can have plugins in this way for both blogs and podcasts and anything, really.
 * <p>
 * // *
 * <p>
 * //
 * each kind of assets - blogs, podcasts, youtube videos - will have their own concept of publishers.
 * <p>
 * each publisher has a unique string associated with them. their plugin-id.
 * <p>
 * the plugin-id can be used to look up credentials from the settings api, decrypt it, and then take the resulting values
 * and bind them to an object - (an arbitrary one? how?) - which we then pass as the context for the plugin itself when we invoke it.
 * <p>
 * there's got to be some sort of enumartion. maybe by bean name? show a drop down of available plugins by their bean name and then store that in the db for the publication? So that it can be involved
 * later on if we suddenly want to, say, delete the thing we published. publications should become a key concept.
 */
@Deprecated
interface PodcastPublicationPlugin {
}
