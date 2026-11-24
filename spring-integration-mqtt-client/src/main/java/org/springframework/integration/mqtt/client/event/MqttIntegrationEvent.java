/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
