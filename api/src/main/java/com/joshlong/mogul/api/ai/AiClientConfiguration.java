package com.joshlong.mogul.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.autoconfigure.openai.OpenAiProperties;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class AiClientConfiguration {

    @Bean
    TranscriptionClient transcriptionClient(ObjectMapper om, S3Client s3, AmqpTemplate amqp) {
        //todo should this be a mogul-specific setting or should we just use the same shared one? ditto for the openai key
        var input = "jlong-transcription-input-bucket";
        var output = "jlong-transcription-output-bucket";
        return new TranscriptionClient(om, s3, amqp, input, output);
    }

    @Bean
    AiClient aiClient(org.springframework.ai.client.AiClient aiClient, TranscriptionClient transcriptionClient, OpenAiProperties properties) {
        return new AiClient(new RestTemplateBuilder().build(), aiClient, properties.getApiKey(),
                transcriptionClient);
    }
}
