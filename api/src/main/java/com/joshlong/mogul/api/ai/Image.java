package com.joshlong.mogul.api.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

record Image(@JsonProperty("revised_prompt") String revisedPrompt, URL url) {
}
