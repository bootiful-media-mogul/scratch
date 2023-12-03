package com.joshlong.mogul.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
@EnableConfigurationProperties (GatewayProperties.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes( GatewayProperties properties) {
		var apiHost = properties.api() ;
		//todo outsource this to a property. maybe this is why we _should_ have the config server?
		// so we can define the whole system in one git repo?
		var apiPrefix = "/api/";
		return route()
				.route(path(apiPrefix + "**"), http(  apiHost))
				.filter(TokenRelayFilterFunctions.tokenRelay())
				.before(BeforeFilterFunctions.rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}"))
				.build();
	}

}
