package com.joshlong.mogul.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;

@EnableConfigurationProperties(ApiProperties.class)
@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}


}


interface MogulRepository extends ListCrudRepository<Mogul, Long> {
}

record Mogul(@Id Long id, String username, String email, String clientId) {
}