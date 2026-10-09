/*
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import java.io.Serial;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was not delivered on publish operation.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 */
public class MqttMessageNotDeliveredEvent extends MqttMessageDeliveryEvent {

	@Serial
	private static final long serialVersionUID = 8983514811627569920L;

	private final Throwable exception;

	public MqttMessageNotDeliveredEvent(Object source, int messageId, String clientId,
			int clientInstance, Throwable exception) {

		super(source, messageId, clientId, clientInstance);
		this.exception = exception;
	}

	public Throwable getException() {
		return this.exception;
	}

}
