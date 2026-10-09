/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.core;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;

/**
 * A utility abstraction over MQTT client which can be used in any MQTT-related component
 * without need to handle generic client callbacks, reconnects etc.
 * Using this manager in multiple MQTT integrations will preserve a single connection.
 *
 * @param <T> MQTT client type
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public interface ClientManager<T extends MqttClient> {

	/**
	 * Return the managed client.
	 * @return the managed client.
	 */
	T getClient();

	/**
	 * Return the managed clients isConnected.
	 * @return the managed clients isConnected.
	 */
	boolean isConnected();

	/**
	 * Register a callback for the {@code onConnected} event from the client.
	 * @param connectCallback a {@link ConnectCallback} to register.
	 */
	void addCallback(ConnectCallback connectCallback);

	/**
	 * Remove the callback from registration.
	 * @param connectCallback a {@link ConnectCallback} to unregister.
	 * @return true if callback was removed.
	 */
	boolean removeCallback(ConnectCallback connectCallback);

	/**
	 * A contract for a custom callback on {@code onConnected} event from the client.
	 *
	 * @see com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener#onConnected(MqttClientConnectedContext)
	 */
	@FunctionalInterface
	interface ConnectCallback {

		/**
		 * Called when the client to the server is connected successfully.
		 * @param mqttClientConnectedContext the mqttClientConnectedContext.
		 */
		void onClientConnected(MqttClientConnectedContext mqttClientConnectedContext);

	}

}
