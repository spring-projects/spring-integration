/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.outbound;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarOperations;
import org.springframework.pulsar.core.PulsarOperations.SendMessageBuilder;
import org.springframework.pulsar.support.PulsarHeaders;
import org.springframework.pulsar.support.PulsarNull;
import org.springframework.pulsar.support.header.JsonPulsarHeaderMapper;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.messaging.MessageHandler} that sends the payload of a
 * message to Apache Pulsar using the provided {@link PulsarOperations}, typically a
 * {@link org.springframework.pulsar.core.PulsarTemplate}.
 * <p>
 * The topic is optional: without a {@link #setTopic(String) topic} or a
 * {@link #setTopicExpression(Expression) topic expression}, or when the expression
 * evaluates to {@code null}, the default topic of the template is used. The
 * {@link PulsarHeaders#TOPIC_NAME} header of a message that was received from Pulsar is
 * deliberately not used as the topic, so that a message is not sent back to the topic
 * it was received from.
 * <p>
 * The message key is taken from the {@link PulsarHeaders#KEY} header unless a
 * {@link #setMessageKeyExpression(Expression) message key expression} is provided. The
 * {@link PulsarHeaders#ORDERING_KEY} and {@link PulsarHeaders#EVENT_TIME} headers are
 * applied when present. The other headers are converted to message properties by the
 * {@link #setHeaderMapper(PulsarHeaderMapper) header mapper}. The sequence id of a
 * received message is never reused because it is specific to the producer that sent it.
 * <p>
 * A {@link org.springframework.pulsar.support.PulsarNull#INSTANCE} value, which the
 * {@link org.springframework.integration.pulsar.inbound.PulsarMessageProducer} creates
 * for a Pulsar message without a value, is sent as a {@code null} value, so that such a
 * message can be forwarded.
 * <p>
 * By default, the message is sent asynchronously and the result is reported to the
 * {@link #setSendSuccessChannel(MessageChannel) success channel}, if any, as the message
 * that was sent with the {@link PulsarHeaders#MESSAGE_ID} header, or to the
 * {@link #setSendFailureChannel(MessageChannel) failure channel}, which defaults to the
 * {@code errorChannel}, as an {@link org.springframework.messaging.support.ErrorMessage}
 * with a {@link MessageHandlingException} that has the failed message. When
 * {@link #setSync(boolean) sync}, the call blocks until the broker has persisted the
 * message and a failure is thrown as a {@link MessageHandlingException}.
 *
 * @param <T> the type of the values that are sent
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 *
 * @see PulsarOperations
 */
public class PulsarMessageHandler<T> extends AbstractMessageHandler {

	private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final Expression DEFAULT_PAYLOAD_EXPRESSION = EXPRESSION_PARSER.parseExpression("payload");

	private static final Expression DEFAULT_MESSAGE_KEY_EXPRESSION =
			new FunctionExpression<Message<?>>((message) -> message.getHeaders().get(PulsarHeaders.KEY));

	private final PulsarOperations<T> pulsarOperations;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private PulsarHeaderMapper headerMapper = JsonPulsarHeaderMapper.builder().build();

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private Expression payloadExpression = DEFAULT_PAYLOAD_EXPRESSION;

	private Expression messageKeyExpression = DEFAULT_MESSAGE_KEY_EXPRESSION;

	private @Nullable Expression topicExpression;

	private @Nullable Schema<T> schema;

	private @Nullable ProducerBuilderCustomizer<T> producerCustomizer;

	private @Nullable MessageChannel sendSuccessChannel;

	private @Nullable String sendSuccessChannelName;

	private @Nullable MessageChannel sendFailureChannel;

	private @Nullable String sendFailureChannelName;

	private boolean sync;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	/**
	 * Create an instance that sends the messages using the provided operations.
	 * @param pulsarOperations the operations to send the messages with, such as a
	 * {@link org.springframework.pulsar.core.PulsarTemplate}.
	 */
	public PulsarMessageHandler(PulsarOperations<T> pulsarOperations) {
		Assert.notNull(pulsarOperations, "'pulsarOperations' must not be null");
		this.pulsarOperations = pulsarOperations;
	}

	/**
	 * Set the topic to send the messages to. By default, the default topic of the
	 * template is used.
	 * @param topic the topic name.
	 */
	public void setTopic(String topic) {
		Assert.hasText(topic, "'topic' must not be empty");
		this.topicExpression = new LiteralExpression(topic);
	}

	/**
	 * Set an expression to evaluate against the message to get the topic to send it to.
	 * When it evaluates to {@code null}, the default topic of the template is used.
	 * @param topicExpression the expression.
	 */
	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' must not be null");
		this.topicExpression = topicExpression;
	}

	/**
	 * Set an expression to evaluate against the message to get the value to send. The
	 * default is the payload of the message.
	 * @param payloadExpression the expression.
	 */
	public void setPayloadExpression(Expression payloadExpression) {
		Assert.notNull(payloadExpression, "'payloadExpression' must not be null");
		this.payloadExpression = payloadExpression;
	}

	/**
	 * Set an expression to evaluate against the message to get its key. The default is
	 * the {@link PulsarHeaders#KEY} header.
	 * @param messageKeyExpression the expression.
	 */
	public void setMessageKeyExpression(Expression messageKeyExpression) {
		Assert.notNull(messageKeyExpression, "'messageKeyExpression' must not be null");
		this.messageKeyExpression = messageKeyExpression;
	}

	/**
	 * Set the {@link Schema} to send the values with. By default, the schema is resolved
	 * by the template from the type of the value.
	 * @param schema the schema.
	 */
	public void setSchema(Schema<T> schema) {
		Assert.notNull(schema, "'schema' must not be null");
		this.schema = schema;
	}

	/**
	 * Set a customizer for the producer that sends the messages, for example to set its
	 * name, its batching or its compression, in addition to the configuration of the
	 * producer factory of the template.
	 * @param producerCustomizer the customizer.
	 */
	public void setProducerCustomizer(ProducerBuilderCustomizer<T> producerCustomizer) {
		Assert.notNull(producerCustomizer, "'producerCustomizer' must not be null");
		this.producerCustomizer = producerCustomizer;
	}

	/**
	 * Set the mapper that converts the message headers to Pulsar message properties.
	 * The default is a {@link JsonPulsarHeaderMapper} that maps all the headers, except
	 * for the {@code id}, the {@code timestamp} and the headers that are received from
	 * Pulsar.
	 * @param headerMapper the mapper.
	 */
	public void setHeaderMapper(PulsarHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set to {@code true} to block until the broker has persisted the message and to
	 * throw the failure, if any. The default is {@code false}, which sends the message
	 * asynchronously.
	 * @param sync true to send synchronously.
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	/**
	 * Set the channel to send the message that was successfully sent to, with the
	 * {@link PulsarHeaders#MESSAGE_ID} header added.
	 * @param sendSuccessChannel the channel.
	 */
	public void setSendSuccessChannel(MessageChannel sendSuccessChannel) {
		Assert.notNull(sendSuccessChannel, "'sendSuccessChannel' must not be null");
		this.sendSuccessChannel = sendSuccessChannel;
	}

	/**
	 * Set the name of the channel to send the message that was successfully sent to.
	 * @param sendSuccessChannelName the name of the channel.
	 * @see #setSendSuccessChannel(MessageChannel)
	 */
	public void setSendSuccessChannelName(String sendSuccessChannelName) {
		Assert.hasText(sendSuccessChannelName, "'sendSuccessChannelName' must not be empty");
		this.sendSuccessChannelName = sendSuccessChannelName;
	}

	/**
	 * Set the channel to send an {@link org.springframework.messaging.support.ErrorMessage}
	 * to when an asynchronous send fails. The default is the {@code errorChannel}.
	 * @param sendFailureChannel the channel.
	 */
	public void setSendFailureChannel(MessageChannel sendFailureChannel) {
		Assert.notNull(sendFailureChannel, "'sendFailureChannel' must not be null");
		this.sendFailureChannel = sendFailureChannel;
	}

	/**
	 * Set the name of the channel to send an
	 * {@link org.springframework.messaging.support.ErrorMessage} to when an asynchronous
	 * send fails.
	 * @param sendFailureChannelName the name of the channel.
	 * @see #setSendFailureChannel(MessageChannel)
	 */
	public void setSendFailureChannelName(String sendFailureChannelName) {
		Assert.hasText(sendFailureChannelName, "'sendFailureChannelName' must not be empty");
		this.sendFailureChannelName = sendFailureChannelName;
	}

	/**
	 * Set the strategy that builds the {@link org.springframework.messaging.support.ErrorMessage}
	 * for a failed send.
	 * @param errorMessageStrategy the strategy.
	 */
	public void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' must not be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	@Override
	public String getComponentType() {
		return "pulsar:outbound-channel-adapter";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.outbound_channel_adapter;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		this.messagingTemplate.setBeanFactory(getBeanFactory());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		T value = evaluatePayload(message);
		SendMessageBuilder<T> sendMessageBuilder = this.pulsarOperations.newMessage(value);
		String topic = (this.topicExpression != null)
				? this.topicExpression.getValue(this.evaluationContext, message, String.class)
				: null;
		if (topic != null) {
			sendMessageBuilder.withTopic(topic);
		}
		if (this.schema != null) {
			sendMessageBuilder.withSchema(this.schema);
		}
		if (this.producerCustomizer != null) {
			sendMessageBuilder.withProducerCustomizer(this.producerCustomizer);
		}
		String key = this.messageKeyExpression.getValue(this.evaluationContext, message, String.class);
		Map<String, String> properties = this.headerMapper.toPulsarHeaders(message.getHeaders());
		byte[] orderingKey = orderingKey(message);
		Long eventTime = eventTime(message);
		sendMessageBuilder.withMessageCustomizer((messageBuilder) -> {
			if (key != null) {
				messageBuilder.key(key);
			}
			messageBuilder.properties(properties);
			if (orderingKey != null) {
				messageBuilder.orderingKey(orderingKey);
			}
			if (eventTime != null) {
				messageBuilder.eventTime(eventTime);
			}
		});
		if (this.sync) {
			sendSynchronously(message, sendMessageBuilder);
		}
		else {
			sendAsynchronously(message, sendMessageBuilder);
		}
	}

	@SuppressWarnings("unchecked")
	private @Nullable T evaluatePayload(Message<?> message) {
		Object value = this.payloadExpression.getValue(this.evaluationContext, message);
		return (value == PulsarNull.INSTANCE) ? null : (T) value;
	}

	private static byte @Nullable [] orderingKey(Message<?> message) {
		Object orderingKey = message.getHeaders().get(PulsarHeaders.ORDERING_KEY);
		if (orderingKey instanceof byte[] bytes) {
			return bytes;
		}
		return (orderingKey != null) ? orderingKey.toString().getBytes(StandardCharsets.UTF_8) : null;
	}

	private static @Nullable Long eventTime(Message<?> message) {
		Object eventTime = message.getHeaders().get(PulsarHeaders.EVENT_TIME);
		return (eventTime instanceof Number number) ? number.longValue() : null;
	}

	private void sendSynchronously(Message<?> message, SendMessageBuilder<T> sendMessageBuilder) {
		MessageId messageId;
		try {
			messageId = sendMessageBuilder.send();
		}
		catch (RuntimeException ex) {
			throw new MessageHandlingException(message, "Failed to send a message to Pulsar", ex);
		}
		sendSuccess(message, messageId);
	}

	private void sendAsynchronously(Message<?> message, SendMessageBuilder<T> sendMessageBuilder) {
		CompletableFuture<MessageId> future;
		try {
			future = sendMessageBuilder.sendAsync();
		}
		catch (RuntimeException ex) {
			throw new MessageHandlingException(message, "Failed to send a message to Pulsar", ex);
		}
		future.whenComplete((messageId, failure) -> {
			if (failure == null) {
				sendSuccess(message, messageId);
			}
			else {
				sendFailure(message, failure);
			}
		});
	}

	private void sendSuccess(Message<?> message, MessageId messageId) {
		MessageChannel channel = getSendSuccessChannel();
		if (channel != null) {
			this.messagingTemplate.send(channel,
					getMessageBuilderFactory().fromMessage(message).setHeader(PulsarHeaders.MESSAGE_ID, messageId)
							.build());
		}
	}

	private void sendFailure(Message<?> message, Throwable failure) {
		Throwable cause = (failure instanceof CompletionException && failure.getCause() != null)
				? failure.getCause()
				: failure;
		MessageChannel channel = getSendFailureChannel();
		if (channel != null) {
			this.messagingTemplate.send(channel, this.errorMessageStrategy
					.buildErrorMessage(new MessageHandlingException(message, "Failed to send a message to Pulsar",
							cause), null));
		}
		else {
			logger.error(cause, () -> "Failed to send " + message + " to Pulsar");
		}
	}

	private @Nullable MessageChannel getSendSuccessChannel() {
		if (this.sendSuccessChannel == null && this.sendSuccessChannelName != null) {
			this.sendSuccessChannel = getChannelResolver().resolveDestination(this.sendSuccessChannelName);
		}
		return this.sendSuccessChannel;
	}

	private @Nullable MessageChannel getSendFailureChannel() {
		if (this.sendFailureChannel == null) {
			String channelName = (this.sendFailureChannelName != null)
					? this.sendFailureChannelName
					: IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME;
			try {
				this.sendFailureChannel = getChannelResolver().resolveDestination(channelName);
			}
			catch (DestinationResolutionException ex) {
				logger.debug(ex, () -> "No channel '" + channelName + "' to report a failed send to");
			}
		}
		return this.sendFailureChannel;
	}

}
