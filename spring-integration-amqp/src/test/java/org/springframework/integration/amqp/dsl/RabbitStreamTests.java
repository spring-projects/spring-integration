/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import com.rabbitmq.stream.Environment;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.rabbit.stream.config.SuperStream;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class RabbitStreamTests implements RabbitTestContainer {

	@Autowired
	MessageChannel sendToRabbitStreamChannel;

	@Autowired
	PollableChannel results;

	@Test
	void rabbitStreamWithSpringIntegrationChannelAdapters() {
		var testData = "test data";
		this.sendToRabbitStreamChannel.send(new GenericMessage<>(testData));

		Message<?> receive = results.receive(10_000);

		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testData);
	}

	@Autowired
	MessageChannel sendToRabbitSuperStreamChannel;

	@Test
	void superStreamWithSpringIntegrationChannelAdapters() {
		var testData = "test super data";
		this.sendToRabbitSuperStreamChannel.send(new GenericMessage<>(testData));

		Message<?> receive = results.receive(10_000);

		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testData);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		ConnectionFactory rabbitConnectionFactory() {
			return new CachingConnectionFactory(RabbitTestContainer.amqpPort());
		}

		@Bean
		RabbitTemplate rabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
			return new RabbitTemplate(rabbitConnectionFactory);
		}

		@Bean
		Environment rabbitStreamEnvironment() {
			return Environment.builder()
					.port(RabbitTestContainer.streamPort())
					.build();
		}

		@Bean(initMethod = "initialize")
		RabbitAdmin rabbitAdmin(ConnectionFactory rabbitConnectionFactory) {
			return new RabbitAdmin(rabbitConnectionFactory);
		}

		@Bean
		Queue stream() {
			return QueueBuilder.durable("test.stream1")
					.stream()
					.build();
		}

		@Bean
		@ServiceActivator(inputChannel = "sendToRabbitStreamChannel")
		RabbitStreamMessageHandlerSpec rabbitStreamMessageHandler(Environment env, Queue stream) {
			return RabbitStream.outboundStreamAdapter(env, stream.getName()).sync(true);
		}

		@Bean
		IntegrationFlow rabbitStreamConsumer(Environment env, Queue stream) {
			return IntegrationFlow.from(
							RabbitStream.inboundAdapter(env)
									.streamName(stream.getName()))
					.channel("results")
					.get();
		}

		@Bean
		QueueChannel results() {
			return new QueueChannel();
		}

		@Bean
		SuperStream superStream() {
			return new SuperStream("test.superStream1", 3);
		}

		@Bean
		@ServiceActivator(inputChannel = "sendToRabbitSuperStreamChannel")
		AmqpOutboundChannelAdapterSpec rabbitSuperStreamMessageHandler(RabbitTemplate rabbitTemplate) {
			return Amqp.outboundAdapter(rabbitTemplate)
					.exchangeName("test.superStream1")
					.routingKey("1");
		}

		@Bean
		IntegrationFlow superStreamConsumer(Environment env) {
			return IntegrationFlow.from(
							RabbitStream.inboundAdapter(env)
									.superStream("test.superStream1", "mySuperConsumer"))
					.channel("results")
					.get();
		}

	}

}
