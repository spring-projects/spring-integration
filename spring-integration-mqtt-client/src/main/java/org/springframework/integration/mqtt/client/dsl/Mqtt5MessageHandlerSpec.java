/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import java.util.Map;
import java.util.function.Function;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.outbound.Mqtt5MessageHandler;
import org.springframework.messaging.Message;

/**
 * A spec for an MQTT 5 outbound message handler.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public class Mqtt5MessageHandlerSpec extends MqttMessageHandlerSpec<Mqtt5MessageHandlerSpec, Mqtt5MessageHandler> {

	Mqtt5MessageHandlerSpec(ClientManager<Mqtt5Client> clientManager) {
		super(new Mqtt5MessageHandler(clientManager));
	}

	/**
	 * Set the expression evaluated against each outgoing message for contentType.
	 * @param contentTypeExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec contentTypeExpression(String contentTypeExpression) {
		this.target.setContentTypeExpressionString(contentTypeExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for contentType.
	 * @param contentTypeExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec contentTypeExpression(Expression contentTypeExpression) {
		this.target.setContentTypeExpression(contentTypeExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for contentType.
	 * @param <P> the message payload type.
	 * @param contentTypeFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec contentTypeFunction(
			Function<Message<P>, ? extends @Nullable String> contentTypeFunction) {

		return contentTypeExpression(new FunctionExpression<>(contentTypeFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for correlationData.
	 * @param correlationDataExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec correlationDataExpression(String correlationDataExpression) {
		this.target.setCorrelationDataExpressionString(correlationDataExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for correlationData.
	 * @param correlationDataExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec correlationDataExpression(Expression correlationDataExpression) {
		this.target.setCorrelationDataExpression(correlationDataExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for correlationData.
	 * @param <P> the message payload type.
	 * @param correlationDataFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec correlationDataFunction(
			Function<Message<P>, ? extends byte @Nullable []> correlationDataFunction) {

		return correlationDataExpression(new FunctionExpression<>(correlationDataFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for responseTopic.
	 * @param responseTopicExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec responseTopicExpression(String responseTopicExpression) {
		this.target.setResponseTopicExpressionString(responseTopicExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for responseTopic.
	 * @param responseTopicExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec responseTopicExpression(Expression responseTopicExpression) {
		this.target.setResponseTopicExpression(responseTopicExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for responseTopic.
	 * @param <P> the message payload type.
	 * @param responseTopicFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec responseTopicFunction(
			Function<Message<P>, ? extends @Nullable String> responseTopicFunction) {

		return responseTopicExpression(new FunctionExpression<>(responseTopicFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for messageExpiryInterval.
	 * @param messageExpiryIntervalExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec messageExpiryIntervalExpression(String messageExpiryIntervalExpression) {
		this.target.setMessageExpiryIntervalExpressionString(messageExpiryIntervalExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for messageExpiryInterval.
	 * @param messageExpiryIntervalExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec messageExpiryIntervalExpression(Expression messageExpiryIntervalExpression) {
		this.target.setMessageExpiryIntervalExpression(messageExpiryIntervalExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for messageExpiryInterval.
	 * @param <P> the message payload type.
	 * @param messageExpiryIntervalFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec messageExpiryIntervalFunction(
			Function<Message<P>, ? extends @Nullable Long> messageExpiryIntervalFunction) {

		return messageExpiryIntervalExpression(new FunctionExpression<>(messageExpiryIntervalFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for payloadFormatIndicator.
	 * @param payloadFormatIndicatorExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec payloadFormatIndicatorExpression(String payloadFormatIndicatorExpression) {
		this.target.setPayloadFormatIndicatorExpressionString(payloadFormatIndicatorExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for payloadFormatIndicator.
	 * @param payloadFormatIndicatorExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec payloadFormatIndicatorExpression(Expression payloadFormatIndicatorExpression) {
		this.target.setPayloadFormatIndicatorExpression(payloadFormatIndicatorExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for payloadFormatIndicator.
	 * @param <P> the message payload type.
	 * @param payloadFormatIndicatorFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec payloadFormatIndicatorFunction(
			Function<Message<P>, ? extends @Nullable Mqtt5PayloadFormatIndicator> payloadFormatIndicatorFunction) {

		return payloadFormatIndicatorExpression(new FunctionExpression<>(payloadFormatIndicatorFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for userProperties.
	 * @param userPropertiesExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec userPropertiesExpression(String userPropertiesExpression) {
		this.target.setUserPropertiesExpressionString(userPropertiesExpression);
		return this;
	}

	/**
	 * Set the expression evaluated against each outgoing message for userProperties.
	 * @param userPropertiesExpression the expression.
	 * @return the spec.
	 * @since 7.3
	 */
	public Mqtt5MessageHandlerSpec userPropertiesExpression(Expression userPropertiesExpression) {
		this.target.setUserPropertiesExpression(userPropertiesExpression);
		return this;
	}

	/**
	 * Set the function evaluated against each outgoing message for userProperties.
	 * @param <P> the message payload type.
	 * @param userPropertiesFunction the function.
	 * @return the spec.
	 * @since 7.3
	 */
	public <P> Mqtt5MessageHandlerSpec userPropertiesFunction(
			Function<Message<P>, ? extends @Nullable Map<?, ?>> userPropertiesFunction) {

		return userPropertiesExpression(new FunctionExpression<>(userPropertiesFunction));
	}

}
