package com.joshlong.mogul.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

@ConfigurationProperties(prefix = "mogul")
public record ApiProperties(Aws aws, Podcasts podcasts, Settings settings, boolean debug) {

	public record Settings(String password, String salt) {
	}

	public record Aws(String accessKey, String accessKeySecret, String region) {
	}

	public record Podcasts(Pipeline pipeline, Processor processor, Aws aws) {

		public record Pipeline(File root) {

			public File drafts() {
				return new File(this.root(), "drafts");
			}

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