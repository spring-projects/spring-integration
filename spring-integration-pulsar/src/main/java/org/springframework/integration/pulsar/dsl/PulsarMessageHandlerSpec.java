/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.dsl;

import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.pulsar.outbound.PulsarMessageHandler;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.pulsar.core.PulsarOperations;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;

/**
 * A {@link MessageHandlerSpec} implementation for the {@link PulsarMessageHandler}.
 *
 * @param <T> the type of the values that are sent.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public class PulsarMessageHandlerSpec<T>
		extends MessageHandlerSpec<PulsarMessageHandlerSpec<T>, PulsarMessageHandler<T>> {

	PulsarMessageHandlerSpec(PulsarOperations<T> pulsarOperations) {
		this.target = new PulsarMessageHandler<>(pulsarOperations);
	}

	/**
	 * Configure the topic to send the messages to.
	 * @param topic the topic name.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> topic(String topic) {
		this.target.setTopic(topic);
		return this;
	}

	/**
	 * Configure a SpEL expression to determine the topic at runtime against the request
	 * message as the root object of the evaluation context.
	 * @param topicExpression the topic SpEL expression.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> topicExpression(String topicExpression) {
		return topicExpression(PARSER.parseExpression(topicExpression));
	}

	/**
	 * Configure an {@link Expression} to determine the topic at runtime against the
	 * request message as the root object of the evaluation context.
	 * @param topicExpression the topic expression.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> topicExpression(Expression topicExpression) {
		this.target.setTopicExpression(topicExpression);
		return this;
	}

	/**
	 * Configure a {@link Function} that is invoked at runtime to determine the topic to
	 * send a message to.
	 * @param topicFunction the topic function.
	 * @param <P> the expected payload type.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public <P> PulsarMessageHandlerSpec<T> topic(Function<Message<P>, String> topicFunction) {
		return topicExpression(new FunctionExpression<>(topicFunction));
	}

	/**
	 * Configure a SpEL expression to determine the value to send at runtime against the
	 * request message as the root object of the evaluation context.
	 * @param payloadExpression the payload SpEL expression.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> payloadExpression(String payloadExpression) {
		this.target.setPayloadExpression(PARSER.parseExpression(payloadExpression));
		return this;
	}

	/**
	 * Configure the key of the messages.
	 * @param messageKey the message key.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> messageKey(String messageKey) {
		return messageKeyExpression(new LiteralExpression(messageKey));
	}

	/**
	 * Configure a SpEL expression to determine the message key at runtime against the
	 * request message as the root object of the evaluation context.
	 * @param messageKeyExpression the message key SpEL expression.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> messageKeyExpression(String messageKeyExpression) {
		return messageKeyExpression(PARSER.parseExpression(messageKeyExpression));
	}

	/**
	 * Configure an {@link Expression} to determine the message key at runtime against the
	 * request message as the root object of the evaluation context.
	 * @param messageKeyExpression the message key expression.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> messageKeyExpression(Expression messageKeyExpression) {
		this.target.setMessageKeyExpression(messageKeyExpression);
		return this;
	}

	/**
	 * Configure the mapper of the message headers to the Pulsar message properties.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> headerMapper(PulsarHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

	/**
	 * Configure whether the messages are sent synchronously.
	 * @param sync true to block until the message is sent.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> sync(boolean sync) {
		this.target.setSync(sync);
		return this;
	}

	/**
	 * Configure the channel to send the messages that were sent successfully to.
	 * @param sendSuccessChannel the channel.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> sendSuccessChannel(MessageChannel sendSuccessChannel) {
		this.target.setSendSuccessChannel(sendSuccessChannel);
		return this;
	}

	/**
	 * Configure the name of the channel to send the messages that were sent successfully
	 * to.
	 * @param sendSuccessChannelName the name of the channel.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> sendSuccessChannelName(String sendSuccessChannelName) {
		this.target.setSendSuccessChannelName(sendSuccessChannelName);
		return this;
	}

	/**
	 * Configure the channel to send the error messages of the failed sends to.
	 * @param sendFailureChannel the channel.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> sendFailureChannel(MessageChannel sendFailureChannel) {
		this.target.setSendFailureChannel(sendFailureChannel);
		return this;
	}

	/**
	 * Configure the name of the channel to send the error messages of the failed sends
	 * to.
	 * @param sendFailureChannelName the name of the channel.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> sendFailureChannelName(String sendFailureChannelName) {
		this.target.setSendFailureChannelName(sendFailureChannelName);
		return this;
	}

	/**
	 * Configure the strategy that builds the error messages of the failed sends.
	 * @param errorMessageStrategy the strategy.
	 * @return the spec.
	 */
	public PulsarMessageHandlerSpec<T> errorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		this.target.setErrorMessageStrategy(errorMessageStrategy);
		return this;
	}

}
