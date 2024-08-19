/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Jiri Soucek
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
	 * Return the managed clients isConnected.
	 * @return the managed clients isConnected.
	 * @since 6.4
	 */
	boolean isConnected();

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
