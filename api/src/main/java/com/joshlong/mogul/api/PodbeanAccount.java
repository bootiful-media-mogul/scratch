package com.joshlong.mogul.api;

import org.springframework.data.annotation.Id;

public record PodbeanAccount(@Id Long id, String clientId, String clientSecret) {
}
