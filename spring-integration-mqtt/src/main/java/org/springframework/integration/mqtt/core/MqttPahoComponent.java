/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * An extension of {@link MqttComponent} for Eclipse Paho components.
 *
 * @author Gary Russell
 * @author Jaehong Park
 * @since 5.4
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
public interface MqttPahoComponent extends MqttComponent<MqttConnectOptions> {

	@Override
	MqttConnectOptions getConnectionInfo();

}
