/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.nio.charset.StandardCharsets;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageNotDeliveredEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT Outbound Adapters.
 *
 * @param <T> MQTT Client type
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.2
 */
public abstract class AbstractMqttMessageHandler<T extends MqttClient> extends AbstractMessageHandler
		implements ApplicationEventPublisherAware {

	private final ClientManager<T> mqttClientManager;

	@SuppressWarnings("NullAway.Init")
	private T mqttClient;

	@SuppressWarnings("NullAway.Init")
	private ApplicationEventPublisher applicationEventPublisher;

	@SuppressWarnings("NullAway.Init")
	private MessageConverter messageConverter;

	@SuppressWarnings("NullAway.Init")
	private StandardEvaluationContext evaluationContext;

	private @Nullable String defaultTopic;

	private MqttQos defaultQos = MqttQos.AT_MOST_ONCE;

	private boolean defaultRetained;

	private Expression topicExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.TOPIC));

	private Expression qosExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.QOS));

	private Expression retainedExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.RETAINED));

	private boolean async;

	protected AbstractMqttMessageHandler(ClientManager<T> mqttClientManager) {
		this.mqttClientManager = mqttClientManager;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Set the message converter to use.
	 * @param messageConverter the messageConverter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'converter' cannot be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Set to true if you don't want to block when sending messages. Default false.
	 * When true, message sent/delivered events will be published for reception
	 * by a suitably configured 'ApplicationListener' or an event
	 * inbound-channel-adapter.
	 * @param async true for async.
	 */
	public void setAsync(boolean async) {
		this.async = async;
	}

	/**
	 * Set the topic to which the message will be published if the
	 * {@link #setTopicExpression(Expression) topicExpression} evaluates to `null`.
	 * @param defaultTopic the default topic.
	 */
	public void setDefaultTopic(String defaultTopic) {
		Assert.hasText(defaultTopic, "'defaultTopic' must not be empty");
		this.defaultTopic = defaultTopic;
	}

	/**
	 * Set the topic expression; default {@code headers['mqtt_topic']}.
	 * @param topicExpression the expression.
	 */
	public void setTopicExpression(Expression topicExpression) {
		Assert.notNull(topicExpression, "'topicExpression' cannot be null");
		this.topicExpression = topicExpression;
	}

	/**
	 * Set the topic expression; default {@code headers['mqtt_topic']}.
	 * @param topicExpression the expression.
	 */
	public void setTopicExpressionString(String topicExpression) {
		Assert.hasText(topicExpression, "'topicExpression' must not be null or empty");
		setTopicExpression(EXPRESSION_PARSER.parseExpression(topicExpression));
	}

	/**
	 * Set the qos for messages if the {@link #setQosExpression(Expression) qosExpression} evaluates to null.
	 * @param defaultQos the default qos.
	 */
	public void setDefaultQos(MqttQos defaultQos) {
		Assert.notNull(defaultQos, "'defaultQos' cannot be null");
		this.defaultQos = defaultQos;
	}

	/**
	 * Set the qos expression; default {@code headers['mqtt_qos']}.
	 * @param qosExpression the expression.
	 */
	public void setQosExpression(Expression qosExpression) {
		Assert.notNull(qosExpression, "'qosExpression' cannot be null");
		this.qosExpression = qosExpression;
	}

	/**
	 * Set the qos expression; default {@code headers['mqtt_qos']}.
	 * @param qosExpression the expression.
	 */
	public void setQosExpressionString(String qosExpression) {
		Assert.hasText(qosExpression, "'qosExpression' must not be null or empty");
		setQosExpression(EXPRESSION_PARSER.parseExpression(qosExpression));
	}

	/**
	 * Set the retained boolean for messages if the {@link #setRetainedExpression(Expression) retainedExpression}
	 * evaluates to null.
	 * @param defaultRetained the defaultRetained.
	 */
	public void setDefaultRetained(boolean defaultRetained) {
		this.defaultRetained = defaultRetained;
	}

	/**
	 * Set the retained expression; default {@code headers['mqtt_retained']}.
	 * @param retainedExpression the expression.
	 */
	public void setRetainedExpression(Expression retainedExpression) {
		Assert.notNull(retainedExpression, "'retainedExpression' cannot be null");
		this.retainedExpression = retainedExpression;
	}

	/**
	 * Set the retained expression; default {@code headers['mqtt_retained']}.
	 * @param retainedExpression the expression.
	 */
	public void setRetainedExpressionString(String retainedExpression) {
		Assert.hasText(retainedExpression, "'retainedExpression' must not be null or empty");
		setRetainedExpression(EXPRESSION_PARSER.parseExpression(retainedExpression));
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.mqttClient = this.mqttClientManager.getClient();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.messageConverter == null) {
			String messageConverterBeanName = IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME;
			setMessageConverter(getBeanFactory().getBean(messageConverterBeanName, SmartMessageConverter.class));
		}
	}

	protected StandardEvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	protected T getClient() {
		return this.mqttClient;
	}

	protected boolean isAsync() {
		return this.async;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object messageForPublish;
		Object payload = message.getPayload();
		if (isMqttMessage(payload)) {
			messageForPublish = payload;
		}
		else {
			String topic = this.topicExpression.getValue(this.evaluationContext, message, String.class);
			if (topic == null) {
				topic = this.defaultTopic;
			}
			Assert.state(topic != null, "No topic could be determined from the message and no default topic defined");

			MqttQos qos = this.qosExpression.getValue(this.evaluationContext, message, MqttQos.class);
			MqttQos qosToUse = qos == null ? this.defaultQos : qos;

			Boolean retained = this.retainedExpression.getValue(this.evaluationContext, message, Boolean.class);
			boolean retainedToUse = retained == null ? this.defaultRetained : retained;

			messageForPublish = buildMqttMessage(topic, getPayloadAsBytes(message), qosToUse, retainedToUse, message);
		}

		publishMqttMessage(messageForPublish, message);
	}

	private byte[] getPayloadAsBytes(Message<?> message) {
		Object payload = message.getPayload();
		byte[] body;
		if (payload instanceof byte[] bytes) {
			body = bytes;
		}
		else if (payload instanceof String string) {
			body = string.getBytes(StandardCharsets.UTF_8);
		}
		else {
			Message<?> convertedMessage = this.messageConverter.toMessage(payload, message.getHeaders());
			Assert.state(convertedMessage != null,
					() -> "The MQTT payload cannot be null. The '" + this.messageConverter +
							"' returned null for: " + message);
			Object convertedPayload = convertedMessage.getPayload();
			if (convertedPayload instanceof byte[] bytes) {
				body = bytes;
			}
			else if (convertedPayload instanceof String string) {
				body = string.getBytes(StandardCharsets.UTF_8);
			}
			else {
				throw new IllegalStateException(
						"The MQTT payload must be a 'byte[]' or 'String'. The '" + this.messageConverter +
						"' returned a '" + convertedPayload.getClass().getSimpleName() + "' for: " + message);
			}
		}
		return body;
	}

	protected abstract boolean isMqttMessage(Object payload);

	protected abstract Object buildMqttMessage(String topic, byte[] body, MqttQos qos,
			boolean retained, Message<?> message);

	protected abstract void publishMqttMessage(Object messageForPublish, Message<?> message);

	protected void messageSentEvent(Message<?> message, Object mqttPublish) {
		this.applicationEventPublisher.publishEvent(new MqttMessageSentEvent(this, message, mqttPublish));
	}

	protected void sendDeliveryCompleteEvent(Object mqttPublishResult) {
		this.applicationEventPublisher.publishEvent(new MqttMessageDeliveredEvent(this, mqttPublishResult));
	}

	protected void sendFailedDeliveryEvent(Object mqttPublishResult, Throwable ex) {
		this.applicationEventPublisher.publishEvent(new MqttMessageNotDeliveredEvent(this, mqttPublishResult, ex));
	}

	@Override
	public String getComponentType() {
		return "mqtt:outbound-channel-adapter";
	}

}
