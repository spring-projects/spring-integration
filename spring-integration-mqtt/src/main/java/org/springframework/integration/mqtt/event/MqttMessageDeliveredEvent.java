/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was delivered.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
@SuppressWarnings("serial")
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
