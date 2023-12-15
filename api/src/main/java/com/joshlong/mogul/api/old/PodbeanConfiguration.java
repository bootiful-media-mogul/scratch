package com.joshlong.mogul.api.old;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.podbean.PodbeanClient;
import com.joshlong.podbean.token.ClientCredentialsTokenProvider;
import com.joshlong.podbean.token.TokenProvider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	TokenProvider multitenantTokenProvider(MogulService ms, PodcastService podcastService) {
		var tokenProviderClass = TokenProvider.class;
		var proxyFactory = new ProxyFactory();
		proxyFactory.setTargetClass(tokenProviderClass);
		proxyFactory.setInterfaces(tokenProviderClass.getInterfaces());
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice(new MethodInterceptor() {
			@Nullable
			@Override
			public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
				System.out.println(
						"not supported yet! Figure this out and soon!! we need for each mogul to have 0-N Podbean configurations as supported podcast publishing targets!");
				return null;
			}
		});
		/*
		 * proxyFactory.addAdvice((MethodInterceptor) invocation -> { var methodName =
		 * invocation.getMethod().getName(); if (methodName.equalsIgnoreCase("getToken"))
		 * { var currentMogul = ms.getCurrentMogul();
		 * log.debug("do we have a valid Mogul? [" + currentMogul + "]"); var
		 * podbeanAccount = podcastService.getPodbeanAccountSettings(currentMogul.id());
		 * var podbeanClientId = podbeanAccount.clientId(); var podbeanClientSecret =
		 * podbeanAccount.clientSecret(); var tp = new
		 * ClientCredentialsTokenProvider(podbeanClientId, podbeanClientSecret); return
		 * tp.getToken(); }
		 *
		 * return null; });
		 */

		return (TokenProvider) proxyFactory.getProxy();

	}

}
