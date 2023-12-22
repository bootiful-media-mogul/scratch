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

import java.util.Map;

class ProducingPodcastPublisherPluginBeanPostProcessor implements BeanPostProcessor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final BeanFactory beanFactory;

	ProducingPodcastPublisherPluginBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof PodcastEpisodePublisherPlugin plugin) {
			var proxyFactoryBean = new ProxyFactoryBean();
			proxyFactoryBean.addAdvice((MethodInterceptor) invocation -> {
				var podcastProducer = beanFactory.getBean(PodcastProducer.class);
				var podcastService = beanFactory.getBean(PodcastService.class);
				var publishMethod = invocation.getMethod().getName().equalsIgnoreCase("publish");
				if (publishMethod) {
					var context = (Map<String, String>) invocation.getArguments()[0];
					var episode = (Episode) invocation.getArguments()[1];
					var shouldProduceAudio = episode.producedAudioUpdated().before(episode.producedAudioAssetsUpdated())
							|| episode.producedAudioUpdated().equals(episode.producedAudioAssetsUpdated());
					log.debug("should produce the audio for episode [" + episode + "] from scratch? ["
							+ shouldProduceAudio + "]");
					if (shouldProduceAudio) {
						var producedManagedFile = podcastProducer.produce(episode);
						log.debug("produced the audio for episode [" + episode + "] from scratch to managedFile: ["
								+ producedManagedFile + "]");
					}
					var updatedEpisode = podcastService.getEpisodeById(episode.id());
					plugin.publish(context, updatedEpisode);
					return null;
				}
				return invocation.proceed();
			});
			var targetClass = plugin.getClass();
			proxyFactoryBean.setInterfaces(targetClass.getInterfaces());
			proxyFactoryBean.setTargetClass(targetClass);
			proxyFactoryBean.setTarget(plugin);
			return proxyFactoryBean.getObject();
		}
		return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
	}

}
