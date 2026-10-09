/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.dsl;

import java.util.concurrent.CompletableFuture;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.pulsar.inbound.PulsarMessageProducer;
import org.springframework.integration.pulsar.outbound.PulsarMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.pulsar.core.PulsarOperations;
import org.springframework.pulsar.core.PulsarOperations.SendMessageBuilder;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;
import org.springframework.pulsar.listener.PulsarAcknowledgingMessageListener;
import org.springframework.pulsar.support.PulsarHeaders;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the Java DSL of the Apache Pulsar support.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
@SpringJUnitConfig
@SuppressWarnings("unchecked")
class PulsarDslTests {

	@Autowired
	MessageChannel requests;

	@Autowired
	QueueChannel receivedMessages;

	@Autowired
	PulsarOperations<String> pulsarOperations;

	@Autowired
	SendMessageBuilder<String> sendMessageBuilder;

	@Autowired
	DefaultPulsarMessageListenerContainer<Object> container;

	@Test
	void outboundAdapterSendsTheMessageToTheConfiguredTopic() {
		this.requests.send(MessageBuilder.withPayload("hello").setHeader("destination", "orders").build());

		verify(this.pulsarOperations).newMessage("hello");
		verify(this.sendMessageBuilder).withTopic("orders");
		verify(this.sendMessageBuilder).sendAsync();
	}

	@Test
	void messageDrivenChannelAdapterSendsTheReceivedMessagesToTheOutputChannel() {
		ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
		verify(this.container).setupMessageListener(listener.capture());
		verify(this.container).start();
		Message<Object> record = mock(Message.class);
		given(record.getValue()).willReturn("greeting");
		given(record.getMessageId()).willReturn(MessageId.earliest);
		given(record.getTopicName()).willReturn("greetings");

		((PulsarAcknowledgingMessageListener<Object>) listener.getValue()).received(mock(Consumer.class), record, null);

		org.springframework.messaging.Message<?> received = this.receivedMessages.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("greeting");
		assertThat(received.getHeaders()).containsEntry(PulsarHeaders.TOPIC_NAME, "greetings");
	}

	@Test
	void specsConfigureTheHandlerAndTheProducer() {
		PulsarHeaderMapper headerMapper = mock(PulsarHeaderMapper.class);
		MessageChannel successChannel = new QueueChannel();

		PulsarMessageHandler<String> handler = Pulsar.outboundAdapter(this.pulsarOperations)
				.topic("static-topic")
				.messageKey("static-key")
				.payloadExpression("payload.toUpperCase()")
				.headerMapper(headerMapper)
				.sync(true)
				.sendSuccessChannel(successChannel)
				.sendFailureChannelName("failures")
				.getObject();

		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "sync")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "headerMapper")).isSameAs(headerMapper);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "sendSuccessChannel")).isSameAs(successChannel);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "sendFailureChannelName")).isEqualTo("failures");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "topicExpression.literalValue")).isEqualTo("static-topic");

		PulsarMessageProducer producer = Pulsar.messageDrivenChannelAdapter(this.container)
				.headerMapper(headerMapper)
				.getObject();

		assertThat(TestUtils.<Object>getPropertyValue(producer, "headerMapper")).isSameAs(headerMapper);
	}

	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		PulsarOperations<String> pulsarOperations(SendMessageBuilder<String> sendMessageBuilder) {
			PulsarOperations<String> operations = mock(PulsarOperations.class);
			given(operations.newMessage(any())).willReturn(sendMessageBuilder);
			return operations;
		}

		@Bean
		SendMessageBuilder<String> sendMessageBuilder() {
			SendMessageBuilder<String> builder = mock(SendMessageBuilder.class, Answers.RETURNS_SELF);
			given(builder.sendAsync()).willReturn(CompletableFuture.completedFuture(MessageId.earliest));
			return builder;
		}

		@Bean
		DefaultPulsarMessageListenerContainer<Object> container() {
			return mock(DefaultPulsarMessageListenerContainer.class);
		}

		@Bean
		QueueChannel receivedMessages() {
			return new QueueChannel();
		}

		@Bean
		MessageChannel requests() {
			return MessageChannels.direct().getObject();
		}

		@Bean
		IntegrationFlow outboundFlow(PulsarOperations<String> pulsarOperations) {
			return IntegrationFlow.from(requests())
					.handle(Pulsar.outboundAdapter(pulsarOperations).topicExpression("headers['destination']"))
					.get();
		}

		@Bean
		IntegrationFlow inboundFlow(DefaultPulsarMessageListenerContainer<Object> container) {
			return IntegrationFlow.from(Pulsar.messageDrivenChannelAdapter(container))
					.channel(receivedMessages())
					.get();
		}

	}

}
