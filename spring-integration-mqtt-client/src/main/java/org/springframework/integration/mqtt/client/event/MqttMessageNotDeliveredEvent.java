/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import java.io.Serial;

/**
 * An event emitted (when using aysnc) when the client indicates the message
 * was not delivered on publish operation.
 *
 * @author Artem Bilan
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class MqttMessageNotDeliveredEvent extends MqttMessageDeliveryEvent {

	@Serial
	private static final long serialVersionUID = 8983514811627569920L;

	private final Throwable exception;

	public MqttMessageNotDeliveredEvent(Object source, Object mqttPublishResult, Throwable exception) {
		super(source, mqttPublishResult);
		this.exception = exception;
	}

	public Throwable getException() {
		return this.exception;
	}

}
