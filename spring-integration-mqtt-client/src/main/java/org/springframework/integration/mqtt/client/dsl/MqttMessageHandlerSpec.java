/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import java.util.function.Function;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mqtt.client.outbound.AbstractMqttMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Common options for MQTT outbound adapters.
 *
 * @param <S> the concrete spec type.
 * @param <H> the handler type.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public abstract class MqttMessageHandlerSpec<S extends MqttMessageHandlerSpec<S, H>,
		H extends AbstractMqttMessageHandler<?>> extends MessageHandlerSpec<S, H> {

	/**
	 * Construct a spec for the provided handler.
	 * @param handler the handler.
	 */
	protected MqttMessageHandlerSpec(H handler) {
		this.target = handler;
	}

	/**
	 * Set the topic used when the topic expression evaluates to null.
	 * @param defaultTopic the defaultTopic.
	 * @return the spec.
	 */
	public S defaultTopic(String defaultTopic) {
		this.target.setDefaultTopic(defaultTopic);
		return _this();
	}

	/**
	 * Set the QoS used when the QoS expression evaluates to null.
	 * The default is {@code MqttQos.AT_MOST_ONCE}.
	 * @param defaultQos the defaultQos.
	 * @return the spec.
	 */
	public S defaultQos(MqttQos defaultQos) {
		this.target.setDefaultQos(defaultQos);
		return _this();
	}

	/**
	 * Set the retain flag used when the retained expression evaluates to null.
	 * The default is false.
	 * @param defaultRetained the defaultRetained.
	 * @return the spec.
	 */
	public S defaultRetained(boolean defaultRetained) {
		this.target.setDefaultRetained(defaultRetained);
		return _this();
	}

	/**
	 * Set whether to publish asynchronously; the default is false.
	 * Asynchronous publishing emits sent and delivery events.
	 * @param async the async.
	 * @return the spec.
	 */
	public S async(boolean async) {
		this.target.setAsync(async);
		return _this();
	}

	/**
	 * Set the converter for outgoing payloads.
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 */
	public S messageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Set the expression evaluated against each outgoing message for topic.
	 * @param topicExpression the expression.
	 * @return the spec.
	 */
	public S topicExpression(String topicExpression) {
		this.target.setTopicExpressionString(topicExpression);
		return _this();
	}

	/**
	 * Set the expression evaluated against each outgoing message for topic.
	 * @param topicExpression the expression.
	 * @return the spec.
	 */
	public S topicExpression(Expression topicExpression) {
		this.target.setTopicExpression(topicExpression);
		return _this();
	}

	/**
	 * Set the function evaluated against each outgoing message for topic.
	 * @param <P> the message payload type.
	 * @param topicFunction the function.
	 * @return the spec.
	 */
	public <P> S topicFunction(Function<Message<P>, ? extends @Nullable String> topicFunction) {
		return topicExpression(new FunctionExpression<>(topicFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for qos.
	 * @param qosExpression the expression.
	 * @return the spec.
	 */
	public S qosExpression(String qosExpression) {
		this.target.setQosExpressionString(qosExpression);
		return _this();
	}

	/**
	 * Set the expression evaluated against each outgoing message for qos.
	 * @param qosExpression the expression.
	 * @return the spec.
	 */
	public S qosExpression(Expression qosExpression) {
		this.target.setQosExpression(qosExpression);
		return _this();
	}

	/**
	 * Set the function evaluated against each outgoing message for qos.
	 * @param <P> the message payload type.
	 * @param qosFunction the function.
	 * @return the spec.
	 */
	public <P> S qosFunction(Function<Message<P>, ? extends @Nullable MqttQos> qosFunction) {
		return qosExpression(new FunctionExpression<>(qosFunction));
	}

	/**
	 * Set the expression evaluated against each outgoing message for retained.
	 * @param retainedExpression the expression.
	 * @return the spec.
	 */
	public S retainedExpression(String retainedExpression) {
		this.target.setRetainedExpressionString(retainedExpression);
		return _this();
	}

	/**
	 * Set the expression evaluated against each outgoing message for retained.
	 * @param retainedExpression the expression.
	 * @return the spec.
	 */
	public S retainedExpression(Expression retainedExpression) {
		this.target.setRetainedExpression(retainedExpression);
		return _this();
	}

	/**
	 * Set the function evaluated against each outgoing message for retained.
	 * @param <P> the message payload type.
	 * @param retainedFunction the function.
	 * @return the spec.
	 */
	public <P> S retainedFunction(Function<Message<P>, ? extends @Nullable Boolean> retainedFunction) {
		return retainedExpression(new FunctionExpression<>(retainedFunction));
	}

}
