package com.joshlong.mogul.api.podcasts.publication;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PodcastEpisodePublisherPluginConfiguration {

    @Bean
    BeanPostProcessor podcastProducingBeanPostProcessor() {
        // the idea is that we want to make sure that _all_ PodcastEpisodePublisherPlugins make sure to produce the audio _before_ they run
        return new BeanPostProcessor() {
            //todo
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof PodcastEpisodePublisherPlugin podcastEpisodePublisherPlugin) {
                    var pfb = new ProxyFactoryBean();
                    pfb.addAdvice(new MethodInterceptor() {
                        @Nullable
                        @Override
                        public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
                            return null;
                        }
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
        };
    }
}
