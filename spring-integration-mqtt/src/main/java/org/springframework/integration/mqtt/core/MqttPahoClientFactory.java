/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.core;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface MqttPahoClientFactory {

	/**
	 * Retrieve a client instance.
	 *
	 * @param url The URL.
	 * @param clientId The client id.
	 * @return The client instance.
	 * @throws MqttException Any.
	 */
	IMqttClient getClientInstance(String url, String clientId) throws MqttException;

	/**
	 * Retrieve an async client instance.
	 *
	 * @param url The URL.
	 * @param clientId The client id.
	 * @return The client instance.
	 * @throws MqttException Any.
	 * @since 4.1
	 */
	IMqttAsyncClient getAsyncClientInstance(String url, String clientId) throws MqttException;

	/**
	 * Retrieve the connection options.
	 *
	 * @return The options.
	 */
	MqttConnectOptions getConnectionOptions();

}
