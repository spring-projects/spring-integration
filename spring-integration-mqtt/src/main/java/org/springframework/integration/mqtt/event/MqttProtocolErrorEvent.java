/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.event;

import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * The even representing an MQTT error occurred during client interaction.
 *
 * @author Artem Bilan
 *
 * @since 5.5.5
 *
 * @see org.eclipse.paho.mqttv5.client.MqttCallback#mqttErrorOccurred(MqttException)
 */
@SuppressWarnings("serial")
public class MqttProtocolErrorEvent extends MqttIntegrationEvent {

	public MqttProtocolErrorEvent(Object source, MqttException exception) {
		super(source, exception);
	}

}
