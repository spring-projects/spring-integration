/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
