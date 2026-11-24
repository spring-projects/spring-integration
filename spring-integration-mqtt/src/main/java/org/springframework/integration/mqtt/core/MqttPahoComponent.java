/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * An extension of {@link MqttComponent} for Eclipse Paho components.
 *
 * @author Gary Russell
 * @since 5.4
 *
 */
public interface MqttPahoComponent extends MqttComponent<MqttConnectOptions> {

	@Override
	MqttConnectOptions getConnectionInfo();

}
