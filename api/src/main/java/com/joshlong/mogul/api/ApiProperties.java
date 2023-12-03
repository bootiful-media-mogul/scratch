package com.joshlong.mogul.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mogul.api")
public record ApiProperties() {
}
