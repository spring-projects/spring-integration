/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueBuilder.Overflow;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.amqp.rabbit.junit.RabbitAvailableCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.DisabledIf;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
@SpringJUnitConfig
@RabbitAvailable(queues = "testConfirmOk")
public class AmqpOutboundEndpointTests2 {

	@Test
	void testConfirmOk(@Autowired IntegrationFlow flow, @Autowired RabbitTemplate template) {
		flow.getInputChannel().send(new GenericMessage<>("test", Collections.singletonMap("rk", "testConfirmOk")));
		assertThat(template.receive("testConfirmOk")).isNotNull();
	}

	@Test
	void testWithReturn(@Autowired IntegrationFlow flow) {
		assertThatThrownBy(() -> flow.getInputChannel()
				.send(new GenericMessage<>("test", Collections.singletonMap("rk", "junkjunk"))))
						.isInstanceOf(MessageHandlingException.class)
						.hasMessageStartingWith("Message was returned by the broker");
	}

	@Test
	@DisabledIf("#{systemEnvironment['TRAVIS'] ?: false}") // needs RabbitMQ 3.7
	void testWithReject(@Autowired IntegrationFlow flow, @Autowired RabbitAdmin admin,
			@Autowired RabbitTemplate template) {

		Queue queue = QueueBuilder.nonDurable()
				.autoDelete()
				.maxLength(1)
				.overflow(Overflow.rejectPublish)
				.build();
		admin.declareQueue(queue);
		flow.getInputChannel().send(new GenericMessage<>("test", Collections.singletonMap("rk", queue.getName())));
		assertThatThrownBy(() -> flow.getInputChannel()
				.send(new GenericMessage<>("test", Collections.singletonMap("rk", queue.getName()))))
						.isInstanceOf(MessageHandlingException.class)
						.hasMessageStartingWith("Negative publisher confirm received: ");
		assertThat(template.receive(queue.getName())).isNotNull();
		admin.deleteQueue(queue.getName());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class Config {

		@Bean
		public IntegrationFlow flow(RabbitTemplate template) {
			return f -> f.handle(Amqp.outboundAdapter(template)
					.exchangeName("")
					.routingKeyFunction(msg -> msg.getHeaders().get("rk", String.class))
					.confirmCorrelationFunction(msg -> msg)
					.waitForConfirm());
		}

		@Bean
		public CachingConnectionFactory cf() {
			CachingConnectionFactory ccf = new CachingConnectionFactory(
					RabbitAvailableCondition.getBrokerRunning().getConnectionFactory());
			ccf.setPublisherConfirms(true);
			ccf.setPublisherReturns(true);
			return ccf;
		}

		@Bean
		public RabbitTemplate template(ConnectionFactory cf) {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(cf);
			rabbitTemplate.setMandatory(true);
			rabbitTemplate.setReceiveTimeout(10_000);
			return rabbitTemplate;
		}

		@Bean
		public RabbitAdmin admin(ConnectionFactory cf) {
			return new RabbitAdmin(cf);
		}

	}

}
