package com.joshlong.mogul.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import javax.sql.DataSource;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	JdbcClient jdbcClient(DataSource dataSource) {
		return JdbcClient.create(dataSource);
	}

	@Bean
	RouterFunction<ServerResponse> routes(GatewayProperties properties) {
		var apiHost = properties.api();
		// todo outsource this to a property. maybe this is why we _should_ have the
		// config server?
		// so we can define the whole system in one git repo?
		var apiPrefix = "/api/";
		return route().route(path(apiPrefix + "**"), http(apiHost))
			.filter(TokenRelayFilterFunctions.tokenRelay())
			.before(BeforeFilterFunctions.rewritePath(apiPrefix + "(?<segment>.*)", "/$\\{segment}"))
			.build();
	}

	@Bean
	SecurityFilterChain oauth2SecurityFilterChain(JdbcOAuth2UserService jdbcOAuth2UserService, HttpSecurity http)
			throws Exception {
		http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
		http.oauth2Login(config -> {
			config.userInfoEndpoint(i -> i.oidcUserService(jdbcOAuth2UserService));
		});
		http.oauth2Client(withDefaults());
		return http.build();
	}

	@Bean
	JdbcOAuth2UserService jdbcOAuth2UserService(JdbcClient jdbcClient) {
		return new JdbcOAuth2UserService(jdbcClient);
	}

}

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

}