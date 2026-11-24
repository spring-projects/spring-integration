/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.event;

/**
 * Base class for events related to message delivery. Properties {@link #messageId},
 * {@link #clientId} and {@link #clientInstance} can be used to correlate events.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
@SuppressWarnings("serial")
public abstract class MqttMessageDeliveryEvent extends MqttIntegrationEvent {

	private final int messageId;

	private final String clientId;

	private final int clientInstance;

	public MqttMessageDeliveryEvent(Object source, int messageId, String clientId, int clientInstance) {
		super(source);
		this.messageId = messageId;
		this.clientId = clientId;
		this.clientInstance = clientInstance;
	}

	public int getMessageId() {
		return this.messageId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public int getClientInstance() {
		return this.clientInstance;
	}

}
