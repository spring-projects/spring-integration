/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.springframework.messaging.Message;

/**
 * An event emitted (when using async) when the client indicates that a message
 * has been sent.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
@SuppressWarnings("serial")
public class MqttMessageSentEvent extends MqttMessageDeliveryEvent {

	private final Message<?> message;

	private final String topic;

	public MqttMessageSentEvent(Object source, Message<?> message, String topic, int messageId,
			String clientId, int clientInstance) {
		super(source, messageId, clientId, clientInstance);
		this.message = message;
		this.topic = topic;
	}

	public Message<?> getMessage() {
		return this.message;
	}

	public String getTopic() {
		return this.topic;
	}

	@Override
	public String toString() {
		return "MqttMessageSentEvent [message=" + this.message
				+ ", topic=" + this.topic
				+ ", clientId=" + getClientId()
				+ ", clientInstance=" + getClientInstance()
				+ ", messageId=" + getMessageId()
				+ "]";
	}

}
