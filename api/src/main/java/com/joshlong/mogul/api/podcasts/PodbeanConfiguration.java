package com.joshlong.mogul.api.podcasts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.podbean.PodbeanClient;
import com.joshlong.podbean.SimplePodbeanClient;
import com.joshlong.podbean.token.ClientCredentialsTokenProvider;
import com.joshlong.podbean.token.TokenProvider;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

/**
 * each mogul may have a configured podbean account, typically stored in the DB
 * (encrypted), so this will expose a scoped proxy that's aware of the tenant and will
 * look up and configure a valid {@link PodbeanClient podbean client} for that security
 * tenant.
 */
@Configuration
class PodbeanConfiguration {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Bean
	TokenProvider multitenantTokenProvider() {
		return (TokenProvider) proxyFactory().getProxy();
	}

	private ProxyFactory proxyFactory() {
		var tokenProviderClass = TokenProvider.class;
		var pf = new ProxyFactory();
		pf.setTargetClass(tokenProviderClass);
		pf.setInterfaces(tokenProviderClass.getInterfaces());
		pf.setProxyTargetClass(true);
		pf.addAdvice((MethodInterceptor) invocation -> {
			var methodName = invocation.getMethod().getName();

			log.debug("invoking " + methodName + " on our multitenantTokenProvider");

			if (methodName.equalsIgnoreCase("getToken")) {

				var mogulName = SecurityContextHolder.getContext().getAuthentication().getName();
				var podbeanClientId = "";
				var podbeanClientSecret = "";
				var tp = new ClientCredentialsTokenProvider(podbeanClientId, podbeanClientSecret);

				log.debug("mogul name: " + mogulName);

			}

			return null;
		});

		return pf;

	}

}
