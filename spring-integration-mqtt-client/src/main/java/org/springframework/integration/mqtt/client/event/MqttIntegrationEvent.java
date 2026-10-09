/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.event;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.events.IntegrationEvent;

/**
 * Base class for Mqtt Events.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 7.2
 */
@SuppressWarnings("serial")
public abstract class MqttIntegrationEvent extends IntegrationEvent {

	public MqttIntegrationEvent(Object source) {
		super(source);
	}

	public MqttIntegrationEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
