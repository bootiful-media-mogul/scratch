package com.joshlong.mogul.api;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;

import java.time.format.DateTimeFormatter;

@IntegrationComponentScan
@EnableConfigurationProperties(ApiProperties.class)
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	@Bean
	DateTimeFormatter dateTimeFormatter() {
		return DateTimeFormatter.BASIC_ISO_DATE;
	}

	@Bean
	ApplicationRunner install(MogulService mogul, Settings settings) {
		return args -> {
			var jlong = mogul.getMogulByName("jlong").id();
			var category = "podbean";
			settings.set(jlong, category, "clientId", "");
			settings.set(jlong, category, "clientSecret", "");
		};
	}

}
