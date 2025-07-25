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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jiandong Ma
 *
 * @since 7.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
class InboundIntegrationTests implements RabbitTestContainer {

	static final String QUEUE_IB_TEST = "rabbitAmqpInboundQueue";

	@Autowired
	RabbitAmqpTemplate rabbitAmqpTemplate;

	@Autowired
	AmqpInboundChannelAdapter inboundAdapter;

	@Autowired
	QueueChannel outputChannel;

	@BeforeAll
	static void initQueues() throws IOException, InterruptedException {
		RABBITMQ.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + QUEUE_IB_TEST);
	}

	@AfterAll
	static void deleteQueues() throws IOException, InterruptedException {
		RABBITMQ.execInContainer("rabbitmqadmin", "delete", "queue", "name=" + QUEUE_IB_TEST);
	}

	@Test
	void testHappyFlow() throws ExecutionException, InterruptedException {
		// GIVEN
		Foo messageToSend = new Foo("bar");
		CompletableFuture<Boolean> future = rabbitAmqpTemplate.convertAndSend(QUEUE_IB_TEST, messageToSend);
		Boolean sentSuccess = future.get();
		assertThat(sentSuccess).isTrue();
		inboundAdapter.start();
		// WHEN
		Message<?> message = outputChannel.receive();
		// THEN
		assertThat(message).isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOfSatisfying(Foo.class, foo -> {
					assertThat(foo.bar).isEqualTo("bar");
				});
		inboundAdapter.stop();
	}

	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		Environment environment() {
			return new AmqpEnvironmentBuilder()
					.connectionSettings()
					.port(RabbitTestContainer.amqpPort())
					.environmentBuilder()
					.build();
		}

		@Bean
		AmqpConnectionFactory amqpConnectionFactory() {
			return new SingleAmqpConnectionFactory(environment());
		}

		@Bean
		AmqpInboundChannelAdapter inboundAdapter(AmqpConnectionFactory amqpConnectionFactory) {
			RabbitAmqpListenerContainer container = new RabbitAmqpListenerContainer(amqpConnectionFactory);
			container.setQueueNames(QUEUE_IB_TEST);
			var adapter = new AmqpInboundChannelAdapter(container);
			adapter.setOutputChannel(outputChannel());
			adapter.setMessageConverter(new JacksonJsonMessageConverter());
			return adapter;
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory amqpConnectionFactory) {
			RabbitAmqpTemplate template = new RabbitAmqpTemplate(amqpConnectionFactory);
			template.setMessageConverter(new JacksonJsonMessageConverter());
			return template;
		}

	}

	record Foo(String bar) {

	}

}
