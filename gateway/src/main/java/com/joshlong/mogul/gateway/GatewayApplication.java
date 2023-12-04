package com.joshlong.mogul.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		var apiPrefix = "/api/";
		return rlb
				.routes()
				.route(rs -> rs
						.path(apiPrefix + "**")
						.filters(f -> f
								.tokenRelay()
								.rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}")
						)
						.uri("http://localhost:8080")
				)
				.route(rs -> rs
						.path("/**")
						.uri("http://localhost:5173")
				)
				.build();
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.authorizeExchange((authorize) -> authorize.anyExchange().authenticated())
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.oauth2Login(Customizer.withDefaults())
				.oauth2Client(Customizer.withDefaults())
				.build();
	}
}
/*

class JdbcOAuth2UserService extends OidcUserService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JdbcClient db;

	JdbcOAuth2UserService(JdbcClient db) {
		this.db = db;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		var user = super.loadUser(userRequest);
		var preferredUsername = user.getUserInfo() != null ? user.getUserInfo().getPreferredUsername() : null;
		var username = StringUtils.hasText(preferredUsername) ? preferredUsername : user.getClaims().get("sub");
		var email = user.getEmail();
		var client = user.getClaims().get("azp");
		var sql = """
				insert into mogul(username, email, client_id) values (?,?,?)
				on conflict on constraint mogul_client_id_username_key do update set email = excluded.email
				""";
		this.db.sql(sql).params(username, email, client).update();
		log.debug("created an account for username [" + username + "]");
		return user;
	}

}*/
