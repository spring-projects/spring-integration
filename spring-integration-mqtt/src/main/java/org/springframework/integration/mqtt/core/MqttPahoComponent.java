/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
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
