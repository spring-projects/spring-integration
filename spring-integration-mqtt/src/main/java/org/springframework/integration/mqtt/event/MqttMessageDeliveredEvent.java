/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
