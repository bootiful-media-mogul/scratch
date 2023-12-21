package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.PodcastService;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.podcasts.production.PodcastProducer;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
class PodcastEpisodePublisherPluginConfiguration {

	@Bean
	static ProducingPodcastPublisherPluginBeanPostProcessor podcastProducingBeanPostProcessor(BeanFactory beanFactory) {
		return new ProducingPodcastPublisherPluginBeanPostProcessor(beanFactory);
	}

}

class ProducingPodcastPublisherPluginBeanPostProcessor implements BeanPostProcessor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final BeanFactory beanFactory;

	ProducingPodcastPublisherPluginBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		var clazzName = getClass().getName();
		if (bean instanceof PodcastEpisodePublisherPlugin podcastEpisodePublisherPlugin) {
			var pfb = new ProxyFactoryBean();
			pfb.addAdvice((MethodInterceptor) invocation -> {
				var podcastProducer = beanFactory.getBean(PodcastProducer.class);
				var podcastService = beanFactory.getBean(PodcastService.class);
				log.debug("inside " + clazzName + ".");
				var publishMethod = invocation.getMethod().getName().equalsIgnoreCase("publish");
				log.debug("about to invoke publish method? " + publishMethod);
				// todo some sort of dirty check to avoid production if dependent files
				// haven't changed.
				if (publishMethod) {
					var context = (Map<String, String>) invocation.getArguments()[0];
					var episode = (Episode) invocation.getArguments()[1];
					var producedManagedFile = podcastProducer.produce(episode);
					log.debug("produced the audio file [" + producedManagedFile
							+ "] before publication with the plugin [" + beanName + "]");

					var updatedEpisode = podcastService.getEpisodeById(episode.id());
					podcastEpisodePublisherPlugin.publish(context, updatedEpisode);
					return null;
				}

				return invocation.proceed();
			});
			var targetClass = podcastEpisodePublisherPlugin.getClass();
			for (var i : targetClass.getInterfaces())
				pfb.addInterface(i);
			pfb.setTargetClass(targetClass);
			pfb.setTarget(podcastEpisodePublisherPlugin);
			return pfb.getObject();
		}
		return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
	}

}
