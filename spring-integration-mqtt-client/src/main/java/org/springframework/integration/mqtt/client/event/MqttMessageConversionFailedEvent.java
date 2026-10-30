/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

/**
 * An event emitted by an inbound channel adapter when a received MQTT publish
 * cannot be converted to a message.
 * The {@link #getMqttPublish()} is the original {@code Mqtt3Publish} or {@code Mqtt5Publish}.
 * With manual acknowledgment, the adapter has already acknowledged it.
 *
 * @author WooSeung Kim
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class MqttMessageConversionFailedEvent extends MqttIntegrationEvent {

	private final Object mqttPublish;

	public MqttMessageConversionFailedEvent(Object source, Object mqttPublish, Throwable cause) {
		super(source, cause);
		this.mqttPublish = mqttPublish;
	}

	public Object getMqttPublish() {
		return this.mqttPublish;
	}

}
