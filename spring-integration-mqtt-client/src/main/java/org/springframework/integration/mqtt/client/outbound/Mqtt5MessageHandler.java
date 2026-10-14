/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * The {@link AbstractMqttMessageHandler} implementation for MQTT v5.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5MessageHandler extends AbstractMqttMessageHandler<Mqtt5Client> {

	private Expression contentTypeExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.CONTENT_TYPE));

	private Expression correlationDataExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.CORRELATION_DATA));

	private Expression responseTopicExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.RESPONSE_TOPIC));

	private Expression messageExpiryIntervalExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.MESSAGE_EXPIRY_INTERVAL));

	private Expression payloadFormatIndicatorExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.PAYLOAD_FORMAT_INDICATOR));

	private Expression userPropertiesExpression = new FunctionExpression<Message<?>>(m ->
			m.getHeaders().get(MqttHeaders.USER_PROPERTIES));

	public Mqtt5MessageHandler(ClientManager<Mqtt5Client> mqttClientManager) {
		super(mqttClientManager);
	}

	/**
	 * Set the contentType expression; default "headers['mqtt_contentType']".
	 * @param contentTypeExpression the expression.
	 */
	public void setContentTypeExpression(Expression contentTypeExpression) {
		Assert.notNull(contentTypeExpression, "'contentTypeExpression' cannot be null");
		this.contentTypeExpression = contentTypeExpression;
	}

	/**
	 * Set the contentType expression; default "headers['mqtt_contentType']".
	 * @param contentTypeExpression the expression.
	 */
	public void setContentTypeExpressionString(String contentTypeExpression) {
		Assert.hasText(contentTypeExpression, "'contentTypeExpression' must not be null or empty");
		setContentTypeExpression(EXPRESSION_PARSER.parseExpression(contentTypeExpression));
	}

	/**
	 * Set the correlationData expression; default "headers['mqtt_correlationData']".
	 * @param correlationDataExpression the expression.
	 */
	public void setCorrelationDataExpression(Expression correlationDataExpression) {
		Assert.notNull(correlationDataExpression, "'correlationDataExpression' cannot be null");
		this.correlationDataExpression = correlationDataExpression;
	}

	/**
	 * Set the correlationData expression; default "headers['mqtt_correlationData']".
	 * @param correlationDataExpression the expression.
	 */
	public void setCorrelationDataExpressionString(String correlationDataExpression) {
		Assert.hasText(correlationDataExpression, "'correlationDataExpression' must not be null or empty");
		setCorrelationDataExpression(EXPRESSION_PARSER.parseExpression(correlationDataExpression));
	}

	/**
	 * Set the responseTopic expression; default "headers['mqtt_responseTopic']".
	 * @param responseTopicExpression the expression.
	 */
	public void setResponseTopicExpression(Expression responseTopicExpression) {
		Assert.notNull(responseTopicExpression, "'responseTopicExpression' cannot be null");
		this.responseTopicExpression = responseTopicExpression;
	}

	/**
	 * Set the responseTopic expression; default "headers['mqtt_responseTopic']".
	 * @param responseTopicExpression the expression.
	 */
	public void setResponseTopicExpressionString(String responseTopicExpression) {
		Assert.hasText(responseTopicExpression, "'responseTopicExpression' must not be null or empty");
		setResponseTopicExpression(EXPRESSION_PARSER.parseExpression(responseTopicExpression));
	}

	/**
	 * Set the messageExpiryInterval expression; default "headers['mqtt_messageExpiryInterval']".
	 * @param messageExpiryIntervalExpression the expression.
	 */
	public void setMessageExpiryIntervalExpression(Expression messageExpiryIntervalExpression) {
		Assert.notNull(messageExpiryIntervalExpression, "'messageExpiryIntervalExpression' cannot be null");
		this.messageExpiryIntervalExpression = messageExpiryIntervalExpression;
	}

	/**
	 * Set the messageExpiryInterval expression; default "headers['mqtt_messageExpiryInterval']".
	 * @param messageExpiryIntervalExpression the expression.
	 */
	public void setMessageExpiryIntervalExpressionString(String messageExpiryIntervalExpression) {
		Assert.hasText(messageExpiryIntervalExpression, "'messageExpiryIntervalExpression' must not be null or empty");
		setMessageExpiryIntervalExpression(EXPRESSION_PARSER.parseExpression(messageExpiryIntervalExpression));
	}

	/**
	 * Set the payloadFormatIndicator expression; default "headers['mqtt_payloadFormatIndicator']".
	 * @param payloadFormatIndicatorExpression the expression.
	 */
	public void setPayloadFormatIndicatorExpression(Expression payloadFormatIndicatorExpression) {
		Assert.notNull(payloadFormatIndicatorExpression, "'payloadFormatIndicatorExpression' cannot be null");
		this.payloadFormatIndicatorExpression = payloadFormatIndicatorExpression;
	}

	/**
	 * Set the payloadFormatIndicator expression; default "headers['mqtt_payloadFormatIndicator']".
	 * @param payloadFormatIndicatorExpression the expression.
	 */
	public void setPayloadFormatIndicatorExpressionString(String payloadFormatIndicatorExpression) {
		Assert.hasText(payloadFormatIndicatorExpression, "'payloadFormatIndicatorExpression' must not be null or empty");
		setPayloadFormatIndicatorExpression(EXPRESSION_PARSER.parseExpression(payloadFormatIndicatorExpression));
	}

	/**
	 * Set the userProperties expression; default "headers['mqtt_userProperties']".
	 * @param userPropertiesExpression the expression.
	 */
	public void setUserPropertiesExpression(Expression userPropertiesExpression) {
		Assert.notNull(userPropertiesExpression, "'userPropertiesExpression' cannot be null");
		this.userPropertiesExpression = userPropertiesExpression;
	}

	/**
	 * Set the userProperties expression; default "headers['mqtt_userProperties']".
	 * @param userPropertiesExpression the expression.
	 */
	public void setUserPropertiesExpressionString(String userPropertiesExpression) {
		Assert.hasText(userPropertiesExpression, "'userPropertiesExpression' must not be null or empty");
		setUserPropertiesExpression(EXPRESSION_PARSER.parseExpression(userPropertiesExpression));
	}

	@Override
	protected boolean isMqttMessage(Object payload) {
		return payload instanceof Mqtt5Publish;
	}

	@Override
	protected Object buildMqttMessage(String topic, byte[] body, @Nullable MqttQos qos,
			@Nullable Boolean retained, Message<?> message) {

		final var builder = Mqtt5Publish.builder()
				.topic(topic)
				.payload(body);

		String contentType = this.contentTypeExpression.getValue(getEvaluationContext(), message, String.class);
		byte[] bytes = this.correlationDataExpression.getValue(getEvaluationContext(), message, byte[].class);
		String responseTopic = this.responseTopicExpression.getValue(getEvaluationContext(), message, String.class);
		Long messageExpiryInterval = this.messageExpiryIntervalExpression
				.getValue(getEvaluationContext(), message, Long.class);
		var mqtt5PayloadFormatIndicator = this.payloadFormatIndicatorExpression
				.getValue(getEvaluationContext(), message, Mqtt5PayloadFormatIndicator.class);
		Map<?, ?> userProperties = this.userPropertiesExpression.getValue(getEvaluationContext(), message, Map.class);

		JavaUtils.INSTANCE
				.acceptIfNotNull(qos, builder::qos)
				.acceptIfNotNull(retained, builder::retain)
				.acceptIfNotNull(contentType, builder::contentType)
				.acceptIfCondition(!ObjectUtils.isEmpty(bytes), bytes, builder::correlationData)
				.acceptIfNotNull(responseTopic, builder::responseTopic)
				.acceptIfNotNull(messageExpiryInterval, builder::messageExpiryInterval)
				.acceptIfNotNull(mqtt5PayloadFormatIndicator, builder::payloadFormatIndicator)
				.acceptIfNotNull(userProperties, (Consumer<Map<?, ?>>) map -> {
					List<Mqtt5UserProperty> userPropertyList = new ArrayList<>();
					map.forEach((key, value) -> {
						userPropertyList.add(Mqtt5UserProperty.of(key.toString(), value.toString()));
					});
					builder.userProperties().addAll(userPropertyList).applyUserProperties();
				});

		return builder.build();
	}

	@Override
	protected void publishMqttMessage(Object messageForPublish, Message<?> message) {
		Mqtt5Publish mqtt5Publish = (Mqtt5Publish) messageForPublish;
		if (isAsync()) {
			getClient().toAsync()
					.publish(mqtt5Publish)
					.whenComplete((mqtt5PublishResult, throwable) -> {
						if (throwable != null) {
							sendFailedDeliveryEvent(mqtt5PublishResult, throwable);
						}
						else {
							sendDeliveryCompleteEvent(mqtt5PublishResult);
						}
					});
			messageSentEvent(message, mqtt5Publish);
		}
		else {
			getClient().toBlocking().publish(mqtt5Publish);
		}

	}

}
