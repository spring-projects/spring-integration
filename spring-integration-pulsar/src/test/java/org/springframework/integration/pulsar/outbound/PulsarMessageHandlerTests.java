/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.outbound;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.pulsar.core.PulsarOperations;
import org.springframework.pulsar.core.PulsarOperations.SendMessageBuilder;
import org.springframework.pulsar.core.TypedMessageBuilderCustomizer;
import org.springframework.pulsar.support.PulsarHeaders;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link PulsarMessageHandler}.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
@SuppressWarnings("unchecked")
class PulsarMessageHandlerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static final MessageId MESSAGE_ID = MessageId.earliest;

	private final PulsarOperations<String> pulsarOperations = mock(PulsarOperations.class);

	private final SendMessageBuilder<String> sendMessageBuilder = mock(SendMessageBuilder.class, Answers.RETURNS_SELF);

	private final TypedMessageBuilder<String> typedMessageBuilder = mock(TypedMessageBuilder.class,
			Answers.RETURNS_SELF);

	private final TestApplicationContext beanFactory = TestUtils.createTestApplicationContext();

	@BeforeEach
	void setUp() {
		this.beanFactory.refresh();
		given(this.pulsarOperations.newMessage(any())).willReturn(this.sendMessageBuilder);
		given(this.sendMessageBuilder.sendAsync()).willReturn(CompletableFuture.completedFuture(MESSAGE_ID));
		given(this.sendMessageBuilder.send()).willReturn(MESSAGE_ID);
	}

	@Test
	void payloadIsSentAsynchronouslyToTheDefaultTopicOfTheTemplate() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		verify(this.pulsarOperations).newMessage("hello");
		verify(this.sendMessageBuilder).sendAsync();
		verify(this.sendMessageBuilder, never()).withTopic(any());
		verify(this.sendMessageBuilder, never()).send();
	}

	@Test
	void staticTopicIsUsed() {
		PulsarMessageHandler<String> handler = handler();
		handler.setTopic("orders");

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		verify(this.sendMessageBuilder).withTopic("orders");
	}

	@Test
	void topicExpressionIsEvaluatedAgainstTheMessage() {
		PulsarMessageHandler<String> handler = handler();
		handler.setTopicExpression(PARSER.parseExpression("headers['destination']"));

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader("destination", "invoices").build());

		verify(this.sendMessageBuilder).withTopic("invoices");
	}

	@Test
	void topicExpressionEvaluatingToNullFallsBackToTheDefaultTopicOfTheTemplate() {
		PulsarMessageHandler<String> handler = handler();
		handler.setTopicExpression(PARSER.parseExpression("headers['destination']"));

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		verify(this.sendMessageBuilder, never()).withTopic(any());
	}

	@Test
	void receivedTopicHeaderDoesNotRouteAForwardedMessageBackToItsSourceTopic() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(
				MessageBuilder.withPayload("hello").setHeader(PulsarHeaders.TOPIC_NAME, "source-topic").build());

		verify(this.sendMessageBuilder, never()).withTopic(any());
	}

	@Test
	void payloadExpressionProvidesTheValue() {
		PulsarMessageHandler<String> handler = handler();
		handler.setPayloadExpression(PARSER.parseExpression("payload.toUpperCase()"));

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		verify(this.pulsarOperations).newMessage("HELLO");
	}

	@Test
	void keyHeaderIsUsedByDefault() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader(PulsarHeaders.KEY, "customer-1").build());
		customize();

		verify(this.typedMessageBuilder).key("customer-1");
	}

	@Test
	void messageKeyExpressionIsEvaluatedAgainstTheMessage() {
		PulsarMessageHandler<String> handler = handler();
		handler.setMessageKeyExpression(PARSER.parseExpression("payload"));

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader(PulsarHeaders.KEY, "ignored").build());
		customize();

		verify(this.typedMessageBuilder).key("hello");
	}

	@Test
	void noKeyIsSetWhenThereIsNone() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").build());
		customize();

		verify(this.typedMessageBuilder, never()).key(any());
	}

	@Test
	void headersAreMappedToMessagePropertiesWithoutIdAndTimestamp() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader("trace-id", "abc").build());
		customize();

		ArgumentCaptor<Map<String, String>> properties = ArgumentCaptor.forClass(Map.class);
		verify(this.typedMessageBuilder).properties(properties.capture());
		assertThat(properties.getValue()).containsEntry("trace-id", "abc")
				.doesNotContainKeys("id", "timestamp", PulsarHeaders.KEY);
	}

	@Test
	void customHeaderMapperIsUsed() {
		PulsarHeaderMapper headerMapper = mock(PulsarHeaderMapper.class);
		given(headerMapper.toPulsarHeaders(any())).willReturn(Map.of("mapped", "value"));
		PulsarMessageHandler<String> handler = handler();
		handler.setHeaderMapper(headerMapper);

		handler.handleMessage(MessageBuilder.withPayload("hello").build());
		customize();

		verify(this.typedMessageBuilder).properties(Map.of("mapped", "value"));
	}

	@Test
	void orderingKeyAndEventTimeHeadersAreApplied() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello")
				.setHeader(PulsarHeaders.ORDERING_KEY, "order-1")
				.setHeader(PulsarHeaders.EVENT_TIME, 1_700_000_000_000L)
				.build());
		customize();

		verify(this.typedMessageBuilder).orderingKey("order-1".getBytes(StandardCharsets.UTF_8));
		verify(this.typedMessageBuilder).eventTime(1_700_000_000_000L);
	}

	@Test
	void orderingKeyAsBytesIsApplied() {
		PulsarMessageHandler<String> handler = handler();
		byte[] orderingKey = { 1, 2, 3 };

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader(PulsarHeaders.ORDERING_KEY, orderingKey)
				.build());
		customize();

		verify(this.typedMessageBuilder).orderingKey(orderingKey);
	}

	@Test
	void sequenceIdOfAReceivedMessageIsNeverReused() {
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").setHeader(PulsarHeaders.SEQUENCE_ID, 7L).build());
		customize();

		verify(this.typedMessageBuilder, never()).sequenceId(7L);
	}

	@Test
	void syncSendBlocksAndReportsTheMessageIdOnTheSuccessChannel() {
		QueueChannel successChannel = new QueueChannel();
		PulsarMessageHandler<String> handler = handler();
		handler.setSync(true);
		handler.setSendSuccessChannel(successChannel);
		Message<String> message = MessageBuilder.withPayload("hello").setHeader("trace-id", "abc").build();

		handler.handleMessage(message);

		verify(this.sendMessageBuilder).send();
		verify(this.sendMessageBuilder, never()).sendAsync();
		Message<?> sent = successChannel.receive(0);
		assertThat(sent).isNotNull();
		assertThat(sent.getPayload()).isEqualTo("hello");
		assertThat(sent.getHeaders()).containsEntry("trace-id", "abc").containsEntry(PulsarHeaders.MESSAGE_ID,
				MESSAGE_ID);
	}

	@Test
	void syncSendFailureIsThrownAsMessageHandlingException() {
		IllegalStateException failure = new IllegalStateException("broker is down");
		given(this.sendMessageBuilder.send()).willThrow(failure);
		PulsarMessageHandler<String> handler = handler();
		handler.setSync(true);
		Message<String> message = MessageBuilder.withPayload("hello").build();

		assertThatExceptionOfType(MessageHandlingException.class).isThrownBy(() -> handler.handleMessage(message))
				.withCause(failure)
				.satisfies((ex) -> assertThat(ex.getFailedMessage()).isSameAs(message));
	}

	@Test
	void asyncSendReportsTheMessageIdOnTheSuccessChannel() {
		QueueChannel successChannel = new QueueChannel();
		PulsarMessageHandler<String> handler = handler();
		handler.setSendSuccessChannel(successChannel);

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		Message<?> sent = successChannel.receive(0);
		assertThat(sent).isNotNull();
		assertThat(sent.getHeaders()).containsEntry(PulsarHeaders.MESSAGE_ID, MESSAGE_ID);
	}

	@Test
	void asyncSendFailureIsReportedOnTheFailureChannelAsErrorMessage() {
		IllegalStateException failure = new IllegalStateException("broker is down");
		given(this.sendMessageBuilder.sendAsync()).willReturn(CompletableFuture.failedFuture(failure));
		QueueChannel failureChannel = new QueueChannel();
		PulsarMessageHandler<String> handler = handler();
		handler.setSendFailureChannel(failureChannel);
		Message<String> message = MessageBuilder.withPayload("hello").build();

		handler.handleMessage(message);

		Message<?> error = failureChannel.receive(0);
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOfSatisfying(MessageHandlingException.class, (ex) -> {
			assertThat(ex.getFailedMessage()).isSameAs(message);
			assertThat(ex.getCause()).isSameAs(failure);
		});
	}

	@Test
	void asyncSendFailureGoesToTheErrorChannelWhenThereIsNoFailureChannel() {
		IllegalStateException failure = new IllegalStateException("broker is down");
		given(this.sendMessageBuilder.sendAsync()).willReturn(CompletableFuture.failedFuture(failure));
		QueueChannel errorChannel = new QueueChannel();
		this.beanFactory.getDefaultListableBeanFactory().destroySingleton("errorChannel");
		this.beanFactory.registerBean("errorChannel", errorChannel);
		PulsarMessageHandler<String> handler = handler();

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		assertThat(errorChannel.receive(0)).isInstanceOf(ErrorMessage.class);
	}

	@Test
	void channelsCanBeConfiguredByName() {
		QueueChannel successChannel = new QueueChannel();
		this.beanFactory.registerBean("successChannel", successChannel);
		PulsarMessageHandler<String> handler = handler();
		handler.setSendSuccessChannelName("successChannel");

		handler.handleMessage(MessageBuilder.withPayload("hello").build());

		assertThat(successChannel.receive(0)).isNotNull();
	}

	@Test
	void componentType() {
		assertThat(handler().getComponentType()).isEqualTo("pulsar:outbound-channel-adapter");
	}

	@Test
	void settersRejectNull() {
		PulsarMessageHandler<String> handler = handler();

		assertThatIllegalArgumentException().isThrownBy(() -> new PulsarMessageHandler<String>(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setTopic(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setTopicExpression(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setPayloadExpression(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setMessageKeyExpression(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setHeaderMapper(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setSendSuccessChannel(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setSendSuccessChannelName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setSendFailureChannel(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setSendFailureChannelName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> handler.setErrorMessageStrategy(null));
	}

	private PulsarMessageHandler<String> handler() {
		PulsarMessageHandler<String> handler = new PulsarMessageHandler<>(this.pulsarOperations);
		handler.setBeanFactory(this.beanFactory);
		handler.afterPropertiesSet();
		return handler;
	}

	private void customize() {
		ArgumentCaptor<TypedMessageBuilderCustomizer<String>> customizer = ArgumentCaptor
				.forClass(TypedMessageBuilderCustomizer.class);
		verify(this.sendMessageBuilder).withMessageCustomizer(customizer.capture());
		customizer.getValue().customize(this.typedMessageBuilder);
	}

}
