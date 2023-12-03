package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.ApiProperties;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class RabbitConfiguration {

	@Bean
	InitializingBean amqpInitializer(AmqpAdmin amqp, ApiProperties p) {
		return () -> Set.of(p.podcasts().processor().amqp().replies(), p.podcasts().processor().amqp().requests())
				.forEach(q -> defineDestination(amqp, q, q, q));
	}

	private void defineDestination(AmqpAdmin amqpAdmin, String exchange, String queue, String routingKey) {

		var q = QueueBuilder.durable(queue).build();
		q.setShouldDeclare(true);
		amqpAdmin.declareQueue(q);

		var e = ExchangeBuilder.topicExchange(exchange).durable(true).build();
		amqpAdmin.declareExchange(e);

		var b = BindingBuilder.bind(q).to(e).with(routingKey).noargs();
		b.setShouldDeclare(true);
		amqpAdmin.declareBinding(b);
	}

}
