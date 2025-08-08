/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
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
