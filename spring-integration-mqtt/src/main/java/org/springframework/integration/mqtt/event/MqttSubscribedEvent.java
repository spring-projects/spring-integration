/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.event;

/**
 * @author Gary Russell
 * @author Jaehong Park
 * @since 4.2.2
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
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
