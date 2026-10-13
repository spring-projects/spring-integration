/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
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

	private MessageProcessor<String> contentTypeProcessor = message -> message.getHeaders()
			.get(MqttHeaders.CONTENT_TYPE, String.class);

	private MessageProcessor<byte[]> correlationDataProcessor = message -> message.getHeaders()
			.get(MqttHeaders.CORRELATION_DATA, byte[].class);

	private MessageProcessor<String> responseTopicProcessor = message -> message.getHeaders()
			.get(MqttHeaders.RESPONSE_TOPIC, String.class);

	private MessageProcessor<Long> messageExpiryIntervalProcessor = message -> message.getHeaders()
			.get(MqttHeaders.MESSAGE_EXPIRY_INTERVAL, Long.class);

	private MessageProcessor<Mqtt5PayloadFormatIndicator> payloadFormatIndicatorProcessor = message ->
			message.getHeaders().get(MqttHeaders.PAYLOAD_FORMAT_INDICATOR, Mqtt5PayloadFormatIndicator.class);

	private MessageProcessor<Map<?, ?>> userPropertiesProcessor = message -> message.getHeaders()
			.get(MqttHeaders.USER_PROPERTIES, Map.class);

	public Mqtt5MessageHandler(ClientManager<Mqtt5Client> mqttClientManager) {
		super(mqttClientManager);
	}

	/**
	 * Set the contentType expression; default "headers['mqtt_contentType']".
	 * @param contentTypeExpression the expression.
	 */
	public void setContentTypeExpression(Expression contentTypeExpression) {
		Assert.notNull(contentTypeExpression, "'contentTypeExpression' cannot be null");
		this.contentTypeProcessor = new ExpressionEvaluatingMessageProcessor<>(contentTypeExpression);
	}

	/**
	 * Set the contentType expression; default "headers['mqtt_contentType']".
	 * @param contentTypeExpression the expression.
	 */
	public void setContentTypeExpressionString(String contentTypeExpression) {
		Assert.hasText(contentTypeExpression, "'contentTypeExpression' must not be null or empty");
		this.contentTypeProcessor = new ExpressionEvaluatingMessageProcessor<>(contentTypeExpression);
	}

	/**
	 * Set the correlationData expression; default "headers['mqtt_correlationData']".
	 * @param correlationDataExpression the expression.
	 */
	public void setCorrelationDataExpression(Expression correlationDataExpression) {
		Assert.notNull(correlationDataExpression, "'correlationDataExpression' cannot be null");
		this.correlationDataProcessor = new ExpressionEvaluatingMessageProcessor<>(correlationDataExpression);
	}

	/**
	 * Set the correlationData expression; default "headers['mqtt_correlationData']".
	 * @param correlationDataExpression the expression.
	 */
	public void setCorrelationDataExpressionString(String correlationDataExpression) {
		Assert.hasText(correlationDataExpression, "'correlationDataExpression' must not be null or empty");
		this.correlationDataProcessor = new ExpressionEvaluatingMessageProcessor<>(correlationDataExpression);
	}

	/**
	 * Set the responseTopic expression; default "headers['mqtt_responseTopic']".
	 * @param responseTopicExpression the expression.
	 */
	public void setResponseTopicExpression(Expression responseTopicExpression) {
		Assert.notNull(responseTopicExpression, "'responseTopicExpression' cannot be null");
		this.responseTopicProcessor = new ExpressionEvaluatingMessageProcessor<>(responseTopicExpression);
	}

	/**
	 * Set the responseTopic expression; default "headers['mqtt_responseTopic']".
	 * @param responseTopicExpression the expression.
	 */
	public void setResponseTopicExpressionString(String responseTopicExpression) {
		Assert.hasText(responseTopicExpression, "'responseTopicExpression' must not be null or empty");
		this.responseTopicProcessor = new ExpressionEvaluatingMessageProcessor<>(responseTopicExpression);
	}

	/**
	 * Set the messageExpiryInterval expression; default "headers['mqtt_messageExpiryInterval']".
	 * @param messageExpiryIntervalExpression the expression.
	 */
	public void setMessageExpiryIntervalExpression(Expression messageExpiryIntervalExpression) {
		Assert.notNull(messageExpiryIntervalExpression, "'messageExpiryIntervalExpression' cannot be null");
		this.messageExpiryIntervalProcessor = new ExpressionEvaluatingMessageProcessor<>(messageExpiryIntervalExpression);
	}

	/**
	 * Set the messageExpiryInterval expression; default "headers['mqtt_messageExpiryInterval']".
	 * @param messageExpiryIntervalExpression the expression.
	 */
	public void setMessageExpiryIntervalExpressionString(String messageExpiryIntervalExpression) {
		Assert.hasText(messageExpiryIntervalExpression, "'messageExpiryIntervalExpression' must not be null or empty");
		this.messageExpiryIntervalProcessor = new ExpressionEvaluatingMessageProcessor<>(messageExpiryIntervalExpression);
	}

	/**
	 * Set the payloadFormatIndicator expression; default "headers['mqtt_payloadFormatIndicator']".
	 * @param payloadFormatIndicatorExpression the expression.
	 */
	public void setPayloadFormatIndicatorExpression(Expression payloadFormatIndicatorExpression) {
		Assert.notNull(payloadFormatIndicatorExpression, "'payloadFormatIndicatorExpression' cannot be null");
		this.payloadFormatIndicatorProcessor = new ExpressionEvaluatingMessageProcessor<>(payloadFormatIndicatorExpression);
	}

	/**
	 * Set the payloadFormatIndicator expression; default "headers['mqtt_payloadFormatIndicator']".
	 * @param payloadFormatIndicatorExpression the expression.
	 */
	public void setPayloadFormatIndicatorExpressionString(String payloadFormatIndicatorExpression) {
		Assert.hasText(payloadFormatIndicatorExpression, "'payloadFormatIndicatorExpression' must not be null or empty");
		this.payloadFormatIndicatorProcessor = new ExpressionEvaluatingMessageProcessor<>(payloadFormatIndicatorExpression);
	}

	/**
	 * Set the userProperties expression; default "headers['mqtt_userProperties']".
	 * @param userPropertiesExpression the expression.
	 */
	public void setUserPropertiesExpression(Expression userPropertiesExpression) {
		Assert.notNull(userPropertiesExpression, "'userPropertiesExpression' cannot be null");
		this.userPropertiesProcessor = new ExpressionEvaluatingMessageProcessor<>(userPropertiesExpression);
	}

	/**
	 * Set the userProperties expression; default "headers['mqtt_userProperties']".
	 * @param userPropertiesExpression the expression.
	 */
	public void setUserPropertiesExpressionString(String userPropertiesExpression) {
		Assert.hasText(userPropertiesExpression, "'userPropertiesExpression' must not be null or empty");
		this.userPropertiesProcessor = new ExpressionEvaluatingMessageProcessor<>(userPropertiesExpression);
	}

	@Override
	protected boolean isMqttMessage(Object payload) {
		return payload instanceof Mqtt5Publish;
	}

	@Override
	protected Object buildMqttMessage(String topic, byte[] body, @Nullable MqttQos qos,
			@Nullable Boolean retained, Message<?> message) {

		var builder = Mqtt5Publish.builder()
				.topic(topic)
				.payload(body);

		if (qos != null) {
			builder = builder.qos(qos);
		}

		if (retained != null) {
			builder = builder.retain(retained);
		}

		String contentType = this.contentTypeProcessor.processMessage(message);
		if (contentType != null) {
			builder = builder.contentType(contentType);
		}

		byte[] bytes = this.correlationDataProcessor.processMessage(message);
		if (!ObjectUtils.isEmpty(bytes)) {
			builder = builder.correlationData(bytes);
		}

		String responseTopic = this.responseTopicProcessor.processMessage(message);
		if (responseTopic != null) {
			builder = builder.responseTopic(responseTopic);
		}

		Long messageExpiryInterval = this.messageExpiryIntervalProcessor.processMessage(message);
		if (messageExpiryInterval != null) {
			builder = builder.messageExpiryInterval(messageExpiryInterval);
		}

		var mqtt5PayloadFormatIndicator = this.payloadFormatIndicatorProcessor.processMessage(message);
		if (mqtt5PayloadFormatIndicator != null) {
			builder = builder.payloadFormatIndicator(mqtt5PayloadFormatIndicator);
		}

		Map<?, ?> userProperties = this.userPropertiesProcessor.processMessage(message);
		if (userProperties != null) {
			List<Mqtt5UserProperty> userPropertyList = new ArrayList<>();
			userProperties.forEach((key, value) -> {
				userPropertyList.add(Mqtt5UserProperty.of(key.toString(), value.toString()));
			});
			builder = builder.userProperties().addAll(userPropertyList).applyUserProperties();
		}

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
