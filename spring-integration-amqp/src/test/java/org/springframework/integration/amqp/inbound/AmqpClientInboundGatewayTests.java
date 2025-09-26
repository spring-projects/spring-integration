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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpClientInboundGatewayTests implements RabbitTestContainer {

	@Autowired
	RabbitAmqpTemplate rabbitTemplate;

	@Autowired
	ContextConfiguration contextConfiguration;

	@Test
	void inboundGatewayExchange() {
		assertThat(this.rabbitTemplate.convertSendAndReceive("q1", "test data"))
				.succeedsWithin(Duration.ofSeconds(10))
				.isEqualTo("TEST DATA");
	}

	@Test
	void inboundGatewayExchangeWithAck() throws InterruptedException {
		org.springframework.amqp.core.Message requestMessage =
				MessageBuilder.withBody("test data #2".getBytes())
						.setMessageId("someMessageId")
						.setContentType(MimeTypeUtils.TEXT_PLAIN_VALUE)
						.build();

		this.rabbitTemplate.send("q2", requestMessage);

		assertThat(this.rabbitTemplate.receive("replyQueue"))
				.succeedsWithin(Duration.ofSeconds(10))
				.satisfies(message -> {
					assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo("someMessageId");
					assertThat(message.getBody()).isEqualTo("TEST DATA #2".getBytes());
				});

		assertThat(this.contextConfiguration.acknowledged.await(10, TimeUnit.SECONDS)).isTrue();
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
		Declarables declarables() {
			return new Declarables(Stream.of("q1", "q2", "replyQueue").map(Queue::new).toArray(Queue[]::new));
		}

		@Bean
		RabbitAmqpTemplate rabbitTemplate(AmqpConnectionFactory connectionFactory) {
			return new RabbitAmqpTemplate(connectionFactory);
		}

		@Bean
		AmqpClientInboundGateway amqpClientInboundGateway(AmqpConnectionFactory connectionFactory) {
			AmqpClientInboundGateway amqpClientInboundGateway = new AmqpClientInboundGateway(connectionFactory, "q1");
			amqpClientInboundGateway.setRequestChannelName("inputChannel");
			return amqpClientInboundGateway;
		}

		@Bean
		AmqpClientInboundGateway manualAckAmqpClientInboundGateway(AmqpConnectionFactory connectionFactory) {
			AmqpClientInboundGateway amqpClientInboundGateway = new AmqpClientInboundGateway(connectionFactory, "q2");
			amqpClientInboundGateway.setRequestChannelName("inputChannel");
			amqpClientInboundGateway.setReplyQueue("replyQueue");
			amqpClientInboundGateway.setAutoSettle(false);
			return amqpClientInboundGateway;
		}

		CountDownLatch acknowledged = new CountDownLatch(1);

		@ServiceActivator(inputChannel = "inputChannel")
		String toUpperCase(String payload,
				@Nullable @Header(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK)
				SimpleAcknowledgment acknowledgment) {

			try {
				return payload.toUpperCase();
			}
			finally {
				if (acknowledgment != null) {
					acknowledgment.acknowledge();
					acknowledged.countDown();
				}
			}
		}

	}

}
