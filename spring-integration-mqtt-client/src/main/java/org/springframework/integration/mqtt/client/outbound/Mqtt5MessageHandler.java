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
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * The {@link AbstractMqttMessageHandler} implementation for MQTT v5.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5MessageHandler extends AbstractMqttMessageHandler<Mqtt5Client> {

	public Mqtt5MessageHandler(ClientManager<Mqtt5Client> mqttClientManager) {
		super(mqttClientManager);
	}

	@Override
	protected boolean isMqttMessage(Object payload) {
		return payload instanceof Mqtt5Publish;
	}

	@Override
	protected Object buildMqttMessage(String topic, byte[] body, @Nullable MqttQos qos, @Nullable Boolean retained, Message<?> message) {
		var builder = Mqtt5Publish.builder()
				.topic(topic)
				.payload(body);

		if (qos != null) {
			builder = builder.qos(qos);
		}

		if (retained != null) {
			builder = builder.retain(retained);
		}

		builder = buildFromMessageHeaders(message, builder);

		return builder.build();
	}

	private Mqtt5PublishBuilder.Complete buildFromMessageHeaders(Message<?> message, Mqtt5PublishBuilder.Complete builder) {
		String contentType = message.getHeaders().get(MqttHeaders.CONTENT_TYPE, String.class);
		if (contentType != null) {
			builder = builder.contentType(contentType);
		}

		byte[] bytes = message.getHeaders().get(MqttHeaders.CORRELATION_DATA, byte[].class);
		if (bytes != null && bytes.length > 0) {
			builder = builder.correlationData(bytes);
		}

		String responseTopic = message.getHeaders().get(MqttHeaders.RESPONSE_TOPIC, String.class);
		if (responseTopic != null) {
			builder = builder.responseTopic(responseTopic);
		}

		Long messageExpiryInterval = message.getHeaders().get(MqttHeaders.MESSAGE_EXPIRY_INTERVAL, Long.class);
		if (messageExpiryInterval != null) {
			builder = builder.messageExpiryInterval(messageExpiryInterval);
		}

		var mqtt5PayloadFormatIndicator = message.getHeaders().get(MqttHeaders.PAYLOAD_FORMAT_INDICATOR, Mqtt5PayloadFormatIndicator.class);
		if (mqtt5PayloadFormatIndicator != null) {
			builder = builder.payloadFormatIndicator(mqtt5PayloadFormatIndicator);
		}

		Map<?, ?> userProperties = message.getHeaders().get(MqttHeaders.USER_PROPERTIES, Map.class);
		if (userProperties != null) {
			List<Mqtt5UserProperty> userPropertyList = new ArrayList<>();
			userProperties.forEach((key, value) -> {
				userPropertyList.add(Mqtt5UserProperty.of(key.toString(), value.toString()));
			});
			builder = builder.userProperties().addAll(userPropertyList).applyUserProperties();
		}
		return builder;
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
			try {
				getClient().toBlocking().publish(mqtt5Publish);
			}
			catch (RuntimeException ex) {
				throw new MessageHandlingException(message, "Failed to publish to MQTT in the [" + this + ']', ex);
			}

		}

	}

}
