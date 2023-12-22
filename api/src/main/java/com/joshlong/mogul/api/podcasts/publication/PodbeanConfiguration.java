package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.Settings;
import com.joshlong.podbean.token.ClientCredentialsTokenProvider;
import com.joshlong.podbean.token.TokenProvider;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
class PodbeanConfiguration {

	@Bean
	TokenProvider multitenantTokenProvider(MogulService mogulService, Settings settings) {
		var log = LoggerFactory.getLogger(getClass());
		var tokenProviderClass = TokenProvider.class;
		var proxyFactory = new ProxyFactory();
		proxyFactory.setTargetClass(tokenProviderClass);
		proxyFactory.setInterfaces(tokenProviderClass.getInterfaces());
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {

			if (!invocation.getMethod().getName().equals("getToken"))
				return invocation.proceed();

			var mogulId = mogulService.getCurrentMogul().id();
			var settingsForTenant = settings.getAllSettingsByCategory(mogulId,
					PodbeanPodcastEpisodePublisherPlugin.PLUGIN_NAME);
			var clientId = settingsForTenant.get("clientId").value();
			var clientSecret = settingsForTenant.get("clientSecret").value();
			Assert.hasText(clientId, "the podbean clientId for mogul [" + mogulId + "] is empty");
			Assert.hasText(clientSecret, "the podbean clientSecret for mogul [" + mogulId + "] is empty");
			log.debug("returning podbean clientId and clientSecret " + ClientCredentialsTokenProvider.class.getName()
					+ " for mogul [" + mogulId + "]");
			return new ClientCredentialsTokenProvider(clientId, clientSecret).getToken();
		});
		return (TokenProvider) proxyFactory.getProxy();
	}

}
