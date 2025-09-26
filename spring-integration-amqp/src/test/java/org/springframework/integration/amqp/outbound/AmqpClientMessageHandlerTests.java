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

package org.springframework.integration.amqp.outbound;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 *
 * @since 7.0
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpClientMessageHandlerTests implements RabbitTestContainer {

	@Autowired
	RabbitAmqpTemplate rabbitTemplate;

	@Autowired
	MessageChannel amqpClientSendChannel;

	@Autowired
	MessageChannel amqpClientSendAndReceiveChannel;

	@Autowired
	AmqpClientMessageHandler amqpClientGateway;

	@Test
	void neitherExchangeAndQueue() {
		Message<String> message = new GenericMessage<>("test data");

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.amqpClientSendChannel.send(message))
				.withRootCauseExactlyInstanceOf(IllegalStateException.class)
				.withStackTraceContaining(
						"For send with defaults, an 'exchange' (and optional 'routingKey') or 'queue' must be provided");
	}

	@Test
	void verifyMessagePublishedProperlyWithCustomHeader() {
		Message<String> message =
				MessageBuilder.withPayload("test data")
						.setHeader("exchange", "e1")
						.setHeader("routingKey", "k1")
						.setHeader("testHeader", "testValue")
						.build();

		this.amqpClientSendChannel.send(message);

		CompletableFuture<org.springframework.amqp.core.Message> received = this.rabbitTemplate.receive("q1");

		assertThat(received).succeedsWithin(Duration.ofSeconds(10))
				.satisfies(m -> {
					// Converted to JSON
					assertThat(m.getBody()).isEqualTo("test data".getBytes());
					assertThat(m.getMessageProperties().getHeaders()).containsEntry("testHeader", "testValue");
				});
	}

	@Test
	void verifySendAndReceive() {
		this.amqpClientGateway.setReturnMessage(false);
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message =
				MessageBuilder.withPayload("request")
						.setReplyChannel(replyChannel)
						.build();

		this.amqpClientSendAndReceiveChannel.send(message);

		this.rabbitTemplate.receiveAndReply("q1", payload -> "reply");

		Message<?> reply = replyChannel.receive(10000);

		assertThat(reply).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("reply");
	}

	@Test
	void verifySendAndReceiveAsMessage() {
		this.amqpClientGateway.setReturnMessage(true);
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message =
				MessageBuilder.withPayload("request")
						.setReplyChannel(replyChannel)
						.build();

		this.amqpClientSendAndReceiveChannel.send(message);

		this.rabbitTemplate.receiveAndReply("q1", payload -> "reply");

		Message<?> reply = replyChannel.receive(10000);

		assertThat(reply).isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(org.springframework.amqp.core.Message.class)
				.extracting("body")
				.isEqualTo("\"reply\"".getBytes());
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
		DirectExchange e1() {
			return new DirectExchange("e1");
		}

		@Bean
		Queue q1() {
			return new Queue("q1");
		}

		@Bean
		Binding b1(Queue q1, DirectExchange e1) {
			return BindingBuilder.bind(q1).to(e1).with("k1");
		}

		@Bean
		RabbitAmqpTemplate rabbitTemplate(AmqpConnectionFactory connectionFactory) {
			RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
			rabbitAmqpTemplate.setMessageConverter(new JacksonJsonMessageConverter());
			return rabbitAmqpTemplate;
		}

		@Bean
		@ServiceActivator(inputChannel = "amqpClientSendChannel")
		AmqpClientMessageHandler amqpClientMessageHandler(RabbitAmqpTemplate rabbitTemplate) {
			AmqpClientMessageHandler messageHandler = new AmqpClientMessageHandler(rabbitTemplate);
			messageHandler.setExchangeExpressionString("headers[exchange]");
			messageHandler.setRoutingKeyExpressionString("headers[routingKey]");
			return messageHandler;
		}

		@Bean
		@ServiceActivator(inputChannel = "amqpClientSendAndReceiveChannel")
		AmqpClientMessageHandler amqpClientGateway(RabbitAmqpTemplate rabbitTemplate) {
			AmqpClientMessageHandler messageHandler = new AmqpClientMessageHandler(rabbitTemplate);
			messageHandler.setRequiresReply(true);
			messageHandler.setReplyPayloadType(String.class);
			messageHandler.setMessageConverter(new JacksonJsonMessageConverter());
			messageHandler.setQueue("q1");
			return messageHandler;
		}

	}

}
