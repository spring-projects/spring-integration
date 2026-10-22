/*
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * The even representing an MQTT error occurred during client interaction.
 *
 * @author Artem Bilan
 * @author Jaehong Park
 *
 * @since 5.5.5
 *
 * @see org.eclipse.paho.mqttv5.client.MqttCallback#mqttErrorOccurred(MqttException)
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings({"serial", "removal"})
public class MqttProtocolErrorEvent extends MqttIntegrationEvent {

	public MqttProtocolErrorEvent(Object source, MqttException exception) {
		super(source, exception);
	}

}
