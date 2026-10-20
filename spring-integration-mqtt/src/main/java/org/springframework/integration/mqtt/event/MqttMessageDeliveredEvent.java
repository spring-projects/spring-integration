/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was delivered.
 *
 * @author Gary Russell
 * @author Jaehong Park
 * @since 4.1
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
public class MqttMessageDeliveredEvent extends MqttMessageDeliveryEvent {

	public MqttMessageDeliveredEvent(Object source, int messageId, String clientId,
			int clientInstance) {
		super(source, messageId, clientId, clientInstance);
	}

	@Override
	public String toString() {
		return "MqttMessageDeliveredEvent [clientId=" + getClientId()
				+ ", clientInstance=" + getClientInstance()
				+ ", messageId=" + getMessageId()
				+ "]";
	}

}
