/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.messaging.Message;

/**
 * The {@link AbstractMqttMessageHandler} implementation for MQTT v3.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.2
 */
public class Mqtt3MessageHandler extends AbstractMqttMessageHandler<Mqtt3Client> {

	public Mqtt3MessageHandler(ClientManager<Mqtt3Client> mqttClientManager) {
		super(mqttClientManager);
	}

	@Override
	protected boolean isMqttMessage(Object payload) {
		return payload instanceof Mqtt3Publish;
	}

	@Override
	protected Object buildMqttMessage(String topic, byte[] body, MqttQos qos, boolean retained, Message<?> message) {
		return Mqtt3Publish.builder()
				.topic(topic)
				.payload(body)
				.retain(retained)
				.qos(qos)
				.build();
	}

	@Override
	protected void publishMqttMessage(Object messageForPublish, Message<?> message) {
		Mqtt3Publish mqtt3Publish = (Mqtt3Publish) messageForPublish;
		if (isAsync()) {
			getClient().toAsync()
					.publish(mqtt3Publish)
					.whenComplete((mqtt3PublishResult, throwable) -> {
						if (throwable != null) {
							sendFailedDeliveryEvent(mqtt3PublishResult, throwable);
						}
						else {
							sendDeliveryCompleteEvent(mqtt3PublishResult);
						}
					});
			messageSentEvent(message, mqtt3Publish);
		}
		else {
			getClient().toBlocking().publish(mqtt3Publish);
		}
	}

}
