/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import org.springframework.messaging.Message;

/**
 * An event emitted (when using async) when the client indicates that a message
 * has been sent.
 *
 * @author Gary Russell
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class MqttMessageSentEvent extends MqttMessageDeliveryEvent {

	private final Message<?> message;

	public MqttMessageSentEvent(Object source, Message<?> message, Object mqttPublish) {
		super(source, mqttPublish);
		this.message = message;
	}

	public Message<?> getMessage() {
		return this.message;
	}

}
