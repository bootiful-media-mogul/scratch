package com.joshlong.mogul.api.old;

import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.utils.RabbitUtils;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class RabbitConfiguration {

	@Bean
	InitializingBean amqpInitializer(AmqpAdmin amqp, ApiProperties p) {
		return () -> Set.of(p.podcasts().processor().amqp().replies(), p.podcasts().processor().amqp().requests())
			.forEach(q -> RabbitUtils.defineDestination(amqp, q, q, q));
	}

}
