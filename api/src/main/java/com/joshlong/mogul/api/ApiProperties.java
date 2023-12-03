package com.joshlong.mogul.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.io.File;

@ConfigurationProperties(prefix = "mogul")
public record ApiProperties(Aws aws ,Podcasts podcasts, boolean debug) {


    public record Aws(String accessKey, String accessKeySecret, String region) {
    }

    public record Podcasts(Pipeline pipeline, Processor processor, Aws aws) {

        public record Pipeline(File root) {

            public File archives() {
                return new File(this.root(), "archives");
            }
        }



        public record Processor(S3 s3, Amqp amqp) {

            public record S3(String assetsBucket, String inputBucket, String outputBucket) {
            }

            public record Amqp(String requests, String replies) {
            }

        }
    }
}