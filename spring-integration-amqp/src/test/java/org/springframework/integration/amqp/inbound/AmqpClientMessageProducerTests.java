/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.List;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpClientMessageProducerTests implements RabbitTestContainer {

	@Autowired
	RabbitAmqpTemplate rabbitTemplate;

	@Autowired
	QueueChannel inputChannel;

	@Test
	void receiveSimpleMessageFromAmqp() {
		this.rabbitTemplate.convertAndSend("q1", "test data");

		Message<?> receive = this.inputChannel.receive(10_000);

		assertThat(receive)
				.extracting(Message::getPayload)
				.isEqualTo("test data");
	}

	@Test
	void receiveAndAck() {
		this.rabbitTemplate.convertAndSend("q2", "test data #2");

		Message<?> receive = this.inputChannel.receive(10_000);

		assertThat(receive)
				.extracting(Message::getPayload)
				.isEqualTo("test data #2");

		SimpleAcknowledgment acknowledgment = StaticMessageHeaderAccessor.getAcknowledgment(receive);
		assertThat(acknowledgment).isNotNull();
		acknowledgment.acknowledge();
	}

	@Test
	void receiveBatch() {
		this.rabbitTemplate.convertAndSend("q3", "test data #3");
		this.rabbitTemplate.convertAndSend("q3", "test data #4");

		Message<?> receive = this.inputChannel.receive(10_000);

		assertThat(receive)
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.list(Message.class))
				.hasSize(2)
				.extracting(Message::getPayload)
				.contains("test data #3", "test data #4");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		Environment environment() {
			return new AmqpEnvironmentBuilder()
					.connectionSettings()
					.port(RabbitTestContainer.amqpPort())
					.environmentBuilder()
					.build();
		}

		@Bean
		AmqpConnectionFactory connectionFactory(Environment environment) {
			return new SingleAmqpConnectionFactory(environment);
		}

		@Bean
		RabbitAmqpAdmin admin(AmqpConnectionFactory connectionFactory) {
			return new RabbitAmqpAdmin(connectionFactory);
		}

		@Bean
		Queue q1() {
			return new Queue("q1");
		}

		@Bean
		Queue q2() {
			return new Queue("q2");
		}

		@Bean
		Queue q3() {
			return new Queue("q3");
		}

		@Bean
		RabbitAmqpTemplate rabbitTemplate(AmqpConnectionFactory connectionFactory) {
			return new RabbitAmqpTemplate(connectionFactory);
		}

		@Bean
		QueueChannel inputChannel() {
			return new QueueChannel();
		}

		@Bean
		AmqpClientMessageProducer amqpClientMessageProducer(AmqpConnectionFactory connectionFactory,
				QueueChannel inputChannel) {

			AmqpClientMessageProducer amqpClientMessageProducer = new AmqpClientMessageProducer(connectionFactory, "q1");
			amqpClientMessageProducer.setOutputChannel(inputChannel);
			return amqpClientMessageProducer;
		}

		@Bean
		AmqpClientMessageProducer manualAckAmqpClientMessageProducer(AmqpConnectionFactory connectionFactory,
				QueueChannel inputChannel) {

			AmqpClientMessageProducer amqpClientMessageProducer = new AmqpClientMessageProducer(connectionFactory, "q2");
			amqpClientMessageProducer.setOutputChannel(inputChannel);
			amqpClientMessageProducer.setAutoSettle(false);
			return amqpClientMessageProducer;
		}

		@Bean
		AmqpClientMessageProducer batchAmqpClientMessageProducer(AmqpConnectionFactory connectionFactory,
				QueueChannel inputChannel) {

			AmqpClientMessageProducer amqpClientMessageProducer = new AmqpClientMessageProducer(connectionFactory, "q3");
			amqpClientMessageProducer.setOutputChannel(inputChannel);
			amqpClientMessageProducer.setBatchSize(2);
			return amqpClientMessageProducer;
		}

	}

}
