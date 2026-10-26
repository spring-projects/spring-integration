/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.core;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.jspecify.annotations.Nullable;

/**
 * @author Gary Russell
 * @author Jaehong Park
 * @since 4.0
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings("removal")
public interface MqttPahoClientFactory {

	/**
	 * Retrieve a client instance.
	 *
	 * @param url The URL.
	 * @param clientId The client id.
	 * @return The client instance.
	 * @throws MqttException Any.
	 */
	IMqttClient getClientInstance(@Nullable String url, String clientId) throws MqttException;

	/**
	 * Retrieve an async client instance.
	 *
	 * @param url The URL.
	 * @param clientId The client id.
	 * @return The client instance.
	 * @throws MqttException Any.
	 * @since 4.1
	 */
	IMqttAsyncClient getAsyncClientInstance(@Nullable String url, String clientId) throws MqttException;

	/**
	 * Retrieve the connection options.
	 *
	 * @return The options.
	 */
	MqttConnectOptions getConnectionOptions();

}
