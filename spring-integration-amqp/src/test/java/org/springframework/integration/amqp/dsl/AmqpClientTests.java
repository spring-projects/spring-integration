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

package org.springframework.integration.amqp.dsl;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
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
class AmqpClientTests implements RabbitTestContainer {

	@Autowired
	@Qualifier("sendFlow.input")
	MessageChannel sendFlowInput;

	@Autowired
	QueueChannel receiveChannel;

	@Test
	void fromOutboundToInboundChannelAdapters() {
		this.sendFlowInput.send(new GenericMessage<>("test"));

		assertThat(this.receiveChannel.receive(10000))
				.extracting(Message::getPayload)
				.isEqualTo("test");
	}

	@Autowired
	@Qualifier("requestReplyOutboundFlow.input")
	MessageChannel requestReplyOutboundFlowInput;

	@Test
	void requestReplyOverAmqp() {
		QueueChannel replyChannel = new QueueChannel();
		Message<String> requestMessage =
				MessageBuilder.withPayload("hello amqp")
						.setReplyChannel(replyChannel)
						.build();

		this.requestReplyOutboundFlowInput.send(requestMessage);

		assertThat(replyChannel.receive(10000))
				.extracting(Message::getPayload)
				.isEqualTo("HELLO AMQP");
	}

	@Configuration
	@EnableIntegration
	static class ContextConfiguration {

		@Bean
		com.rabbitmq.client.amqp.Environment environment() {
			return new AmqpEnvironmentBuilder()
					.connectionSettings()
					.port(RabbitTestContainer.amqpPort())
					.environmentBuilder()
					.build();
		}

		@Bean
		AmqpConnectionFactory connectionFactory(com.rabbitmq.client.amqp.Environment environment) {
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
		Queue requestReply() {
			return new Queue("requestReply");
		}

		@Bean
		RabbitAmqpTemplate rabbitTemplate(AmqpConnectionFactory connectionFactory) {
			RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
			rabbitAmqpTemplate.setMessageConverter(new JacksonJsonMessageConverter());
			return rabbitAmqpTemplate;
		}

		@Bean
		IntegrationFlow sendFlow(RabbitAmqpTemplate rabbitTemplate) {
			return f -> f
					.handle(AmqpClient.outboundAdapter(rabbitTemplate)
							.exchange("e1")
							.routingKeyExpression("'k1'"));
		}

		@Bean
		IntegrationFlow receiveFlow(AmqpConnectionFactory connectionFactory) {
			return IntegrationFlow.from(AmqpClient.inboundChannelAdapter(connectionFactory, "q1"))
					.channel(c -> c.queue("receiveChannel"))
					.get();
		}

		@Bean
		IntegrationFlow requestReplyOutboundFlow(RabbitAmqpTemplate rabbitTemplate) {
			return f -> f
					.handle(AmqpClient.outboundGateway(rabbitTemplate)
							.queueFunction(m -> "requestReply"));
		}

		@Bean
		IntegrationFlow requestReplyInboundFlow(AmqpConnectionFactory connectionFactory) {
			return IntegrationFlow.from(AmqpClient.inboundGateway(connectionFactory, "requestReply"))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

	}

}
