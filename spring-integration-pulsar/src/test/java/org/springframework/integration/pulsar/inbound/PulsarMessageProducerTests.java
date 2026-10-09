/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.inbound;

import java.util.Map;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.pulsar.support.PulsarIntegrationHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.pulsar.listener.Acknowledgement;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;
import org.springframework.pulsar.listener.PulsarAcknowledgingMessageListener;
import org.springframework.pulsar.support.PulsarHeaders;
import org.springframework.pulsar.support.PulsarNull;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link PulsarMessageProducer}.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
@SuppressWarnings("unchecked")
class PulsarMessageProducerTests {

	private final DefaultPulsarMessageListenerContainer<Object> container = mock(
			DefaultPulsarMessageListenerContainer.class);

	private final Consumer<Object> consumer = mock(Consumer.class);

	private final TestApplicationContext beanFactory = TestUtils.createTestApplicationContext();

	private final QueueChannel outputChannel = new QueueChannel();

	@BeforeEach
	void setUp() {
		this.beanFactory.refresh();
	}

	@Test
	void theListenerIsSetOnTheContainerWhenInitialized() {
		PulsarMessageProducer producer = producer();

		assertThat(listener()).isNotNull();
		assertThat(producer.getComponentType()).isEqualTo("pulsar:message-driven-channel-adapter");
	}

	@Test
	void startAndStopAreDelegatedToTheContainer() {
		PulsarMessageProducer producer = producer();

		producer.start();
		verify(this.container).start();

		producer.stop();
		verify(this.container).stop();
	}

	@Test
	void receivedMessageIsSentToTheOutputChannelWithItsPayloadAndHeaders() {
		producer();
		Message<Object> record = pulsarMessage("hello", "customer-1", "orders", Map.of("trace-id", "abc"));

		listener().received(this.consumer, record, null);

		org.springframework.messaging.Message<?> received = this.outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("hello");
		assertThat(received.getHeaders()).containsEntry(PulsarHeaders.KEY, "customer-1")
				.containsEntry(PulsarHeaders.TOPIC_NAME, "orders")
				.containsEntry("trace-id", "abc")
				.doesNotContainKey(PulsarIntegrationHeaders.ACKNOWLEDGMENT);
	}

	@Test
	void nullValueOfARecordIsTheNullPayloadOfSpringForApachePulsar() {
		producer();

		listener().received(this.consumer, pulsarMessage(null, null, "orders", Map.of()), null);

		org.springframework.messaging.Message<?> received = this.outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isSameAs(PulsarNull.INSTANCE);
	}

	@Test
	void acknowledgementIsExposedAsAHeader() {
		producer();
		Acknowledgement acknowledgement = mock(Acknowledgement.class);

		listener().received(this.consumer, pulsarMessage("hello", null, "orders", Map.of()), acknowledgement);

		org.springframework.messaging.Message<?> received = this.outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsEntry(PulsarIntegrationHeaders.ACKNOWLEDGMENT, acknowledgement);
	}

	@Test
	void customHeaderMapperIsUsed() {
		PulsarHeaderMapper headerMapper = mock(PulsarHeaderMapper.class);
		given(headerMapper.toSpringHeaders(any()))
				.willReturn(new org.springframework.messaging.MessageHeaders(Map.of("mapped", "value")));
		PulsarMessageProducer producer = new PulsarMessageProducer(this.container);
		producer.setHeaderMapper(headerMapper);
		initialize(producer);

		listener().received(this.consumer, pulsarMessage("hello", null, "orders", Map.of()), null);

		org.springframework.messaging.Message<?> received = this.outputChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsEntry("mapped", "value");
	}

	@Test
	void failureToSendIsRoutedToTheErrorChannelWhenThereIsOne() {
		QueueChannel errorChannel = new QueueChannel();
		DirectChannel failingChannel = new DirectChannel();
		failingChannel.subscribe((message) -> {
			throw new IllegalStateException("downstream failure");
		});
		PulsarMessageProducer producer = new PulsarMessageProducer(this.container);
		producer.setOutputChannel(failingChannel);
		producer.setErrorChannel(errorChannel);
		initialize(producer);

		listener().received(this.consumer, pulsarMessage("hello", null, "orders", Map.of()), null);

		org.springframework.messaging.Message<?> error = errorChannel.receive(0);
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOfSatisfying(MessagingException.class,
				(ex) -> assertThat(ex.getFailedMessage()).isNotNull());
	}

	@Test
	void failureToSendIsRethrownWhenThereIsNoErrorChannelSoThatTheContainerCanRedeliver() {
		DirectChannel failingChannel = new DirectChannel();
		failingChannel.subscribe((message) -> {
			throw new IllegalStateException("downstream failure");
		});
		PulsarMessageProducer producer = new PulsarMessageProducer(this.container);
		producer.setOutputChannel(failingChannel);
		initialize(producer);
		Message<Object> record = pulsarMessage("hello", null, "orders", Map.of());
		PulsarAcknowledgingMessageListener<Object> listener = listener();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> listener.received(this.consumer, record, null))
				.withRootCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void constructorAndSetterRejectNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PulsarMessageProducer(null));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PulsarMessageProducer(this.container).setHeaderMapper(null));
	}

	private PulsarMessageProducer producer() {
		PulsarMessageProducer producer = new PulsarMessageProducer(this.container);
		return initialize(producer);
	}

	private PulsarMessageProducer initialize(PulsarMessageProducer producer) {
		if (producer.getOutputChannel() == null) {
			producer.setOutputChannel(this.outputChannel);
		}
		producer.setBeanFactory(this.beanFactory);
		producer.afterPropertiesSet();
		return producer;
	}

	private PulsarAcknowledgingMessageListener<Object> listener() {
		ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
		verify(this.container).setupMessageListener(listener.capture());
		return (PulsarAcknowledgingMessageListener<Object>) listener.getValue();
	}

	private static Message<Object> pulsarMessage(Object value, String key, String topic,
			Map<String, String> properties) {

		Message<Object> message = mock(Message.class);
		given(message.getValue()).willReturn(value);
		given(message.getMessageId()).willReturn(MessageId.earliest);
		given(message.getTopicName()).willReturn(topic);
		given(message.hasKey()).willReturn(key != null);
		given(message.getKey()).willReturn(key);
		given(message.getProperties()).willReturn(properties);
		return message;
	}

}
