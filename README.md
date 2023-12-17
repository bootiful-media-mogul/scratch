# scratch

pardon our dust. 

im going to do all the work here in a monorepo and then factor out as necessary 


## publications 

there will be a concept of a `publication`, which in turn represents the use of a particular plugin to publish a particular asset at a particular time to some publication endpoint. you might publish to the podbean API for the same blog as 10 times. each of those will be a separate publication. we only  care about the latest-and-greatest.

the publication endpoints will vary based on the thing being published. the publication references will be an ordered, 1-N collection of publications managed by the thing being published. so you'll need to ask a podcast, a blog, or video for its publications. the publications won't know to whom they belong. a publication keeps track of the date, the name of the plugin, the published or not status check (so it could be restarted later, if necessary). 

i imagine there should be a base `Publisher` with a `T` context that could be specialized for requests for `Podcast`, `Blog`, etc. Each plugin will have a thing for which its qualified (this plugin handles podcasts, that handles blogs, etc) and the ability to given a pointer to one of those things to publish, unpublish, and update the publication status. eg, if i publish a podcast, i should be able to unpublish it, and then republish it. i should be able to ask about its final url and other information readily reportable on synchronization.

```java 


import java.net.URL;
import java.util.Date;

record PublicationStatus<T>(T payload, boolean published, Date publishedAt, URL url) {
}

record PublicationContext(Mogul mogul, Map<String, Setting> context) {
}


interface Publisher<T> {

    String name();

    // we should not even show the 'next' button in the UI unless this is true and the plugin shoudl not be shown in the menu
    boolean canPublish(PublicationContext context, T thing);

    void publish(PublicationContext context, T thing);

    void unpublish(PublicationContext context, T thing);

    /**
     * no arbitrary  return values, only the common denominator stuff.  
     * we leave as an implementation detail the questions of what other information should be 
     * returned, how it should be displayed, should it be cached in the db, etc.
     */
    PublicationStatus<T> status(PublicationContext context, T thing);
}

```

our abstract plugin will use beanNameAware to provide the name of the plugin.  

Some publishers will require `Mogul` tenant specific configuration, so given the name of the publication plugin, we'll look up the mogul specific settings for the plugin, where the plugin name is the category. 

it'll all get stored in a Map<String, Setting> which well pass into the publisher plugin as the context. can we use spring's bean binder to bind environment variables to a configuration object of the right type? i think a stringly-typed map might be more flexible since we might have old properties that dont map to the new configuration java objects.

the system will enumerate all the plugins compatible with podcasts and show them as a drop down menu once the person has clicked on thye `publish` button.

UIs will be unique fro publication type and plugin, obviously. maybe we just need a mapping betweeen plugin name and vue.js component to router? but where? not on the serever side, id hope. the vue.js client. afer all if i dont have a component for a plugin, i dont want to show the drop down menu offering to load the component to use it.

all the plugins can do is kick off these sometimes long running processes and give us statuss back when something is ongoing. 

wwho does what with the status thought? could we persist the status results and make them durable?  have a proxy-ied persisteing with jdbc implementation of PodbeasnPublisher that knows how to get the status of a publication and persist it meaningfuly in whatever db it needs to? 
