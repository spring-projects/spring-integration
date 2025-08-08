/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.mqtt.core;

import org.springframework.context.SmartLifecycle;

/**
 * A utility abstraction over MQTT client which can be used in any MQTT-related component
 * without need to handle generic client callbacks, reconnects etc.
 * Using this manager in multiple MQTT integrations will preserve a single connection.
 *
 * @param <T> MQTT client type
 * @param <C> MQTT connection options type (v5 or v3)
 *
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 *
 * @since 6.0
 */
public interface ClientManager<T, C> extends SmartLifecycle, MqttComponent<C> {

	/**
	 * The default completion timeout in milliseconds.
	 */
	long DEFAULT_COMPLETION_TIMEOUT = 30_000L;

	/**
	 * The default disconnect completion timeout in milliseconds.
	 */
	long DISCONNECT_COMPLETION_TIMEOUT = 5_000L;

	/**
	 * Return the managed client.
	 * @return the managed client.
	 */
	T getClient();

	/**
	 * If manual acknowledge has to be used; false by default.
	 * @return true if manual acknowledge has to be used.
	 */
	boolean isManualAcks();

	/**
	 * Register a callback for the {@code connectComplete} event from the client.
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
	 * A contract for a custom callback on {@code connectComplete} event from the client.
	 *
	 * @see org.eclipse.paho.mqttv5.client.MqttCallback#connectComplete
	 * @see org.eclipse.paho.client.mqttv3.MqttCallbackExtended#connectComplete
	 */
	@FunctionalInterface
	interface ConnectCallback {

		/**
		 * Called when the connection to the server is completed successfully.
		 * @param isReconnect if true, the connection was the result of automatic reconnect.
		 */
		void connectComplete(boolean isReconnect);

	}

}
