/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.springframework.messaging.Message;

/**
 * An event emitted (when using async) when the client indicates that a message
 * has been sent.
 *
 * @author Gary Russell
 * @author Jaehong Park
 * @since 4.1
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
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
