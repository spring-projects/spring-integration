/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import org.jspecify.annotations.Nullable;

/**
 * The {@link MqttIntegrationEvent} to notify that connection succeeded but subscription failed.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public class MqttProtocolErrorEvent extends MqttIntegrationEvent {

	public MqttProtocolErrorEvent(Object source) {
		super(source);
	}

	public MqttProtocolErrorEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
