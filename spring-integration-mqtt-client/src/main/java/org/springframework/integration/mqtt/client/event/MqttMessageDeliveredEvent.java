/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was delivered.
 *
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class MqttMessageDeliveredEvent extends MqttMessageDeliveryEvent {

	public MqttMessageDeliveredEvent(Object source, Object mqttPublishResult) {
		super(source, mqttPublishResult);
	}

}
