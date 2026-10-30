/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

/**
 * An event emitted by an MQTT v3 inbound channel adapter when a received publish
 * cannot be converted to a message.
 * With manual acknowledgment, the adapter has already acknowledged the publish.
 *
 * @author WooSeung Kim
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class Mqtt3MessageConversionFailedEvent extends MqttIntegrationEvent {

	private final Mqtt3Publish mqttPublish;

	public Mqtt3MessageConversionFailedEvent(Object source, Mqtt3Publish mqttPublish, Throwable cause) {
		super(source, cause);
		this.mqttPublish = mqttPublish;
	}

	public Mqtt3Publish getMqttPublish() {
		return this.mqttPublish;
	}

}
