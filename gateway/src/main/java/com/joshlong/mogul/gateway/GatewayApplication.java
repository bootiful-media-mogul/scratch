package com.joshlong.mogul.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		var apiPrefix = "/api/";
		return rlb.routes()
			.route(rs -> rs.path(apiPrefix + "**")
				.filters(f -> f.tokenRelay().rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}")

				)
				.uri("http://localhost:8080"))
			.route(rs -> rs.path("/**").uri("http://localhost:5173"))
			.build();
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http//
			.authorizeExchange((authorize) -> authorize.anyExchange().authenticated())//
			.csrf(ServerHttpSecurity.CsrfSpec::disable)//
			.oauth2Login(Customizer.withDefaults())//
			.oauth2Client(Customizer.withDefaults())//
			.build();
	}

}