package com.joshlong.mogul.api.blogs;

import com.joshlong.mogul.api.publications.Publication;
import com.joshlong.mogul.api.publications.PublicationStatus;
import com.joshlong.mogul.api.publications.PublisherPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
class GithubBlogPlugin implements PublisherPlugin , BeanNameAware {

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
    public PublicationStatus status(Publication publication) {
        return null;
    }
}
