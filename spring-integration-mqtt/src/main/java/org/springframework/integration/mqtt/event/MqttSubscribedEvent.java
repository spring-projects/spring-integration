/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.event;

/**
 * @author Gary Russell
 * @since 4.2.2
 *
 */
@SuppressWarnings("serial")
public class MqttSubscribedEvent extends MqttIntegrationEvent {

	private final String message;

	public MqttSubscribedEvent(Object source, String message) {
		super(source);
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return "MqttSubscribedEvent [message=" + this.message + ", source=" + source + "]";
	}

}
