/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

/**
 * An event emitted by an MQTT v5 inbound channel adapter when a received publish
 * cannot be converted to a message.
 * With manual acknowledgment, the adapter has already acknowledged the publish.
 *
 * @author WooSeung Kim
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class Mqtt5MessageConversionFailedEvent extends MqttIntegrationEvent {

	private final Mqtt5Publish mqttPublish;

	public Mqtt5MessageConversionFailedEvent(Object source, Mqtt5Publish mqttPublish, Throwable cause) {
		super(source, cause);
		this.mqttPublish = mqttPublish;
	}

	public Mqtt5Publish getMqttPublish() {
		return this.mqttPublish;
	}

}
