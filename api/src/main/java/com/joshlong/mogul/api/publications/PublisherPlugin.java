package com.joshlong.mogul.api.publications;

public interface PublisherPlugin {

    String name();

    boolean supports(Publication publication);

    void publish(Publication publication);

    PublicationStatus status(Publication publication);
}
