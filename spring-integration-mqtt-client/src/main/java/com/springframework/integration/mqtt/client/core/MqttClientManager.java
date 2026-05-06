/*
 * Copyright 2026-present the original author or authors.
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

package com.springframework.integration.mqtt.client.core;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;

import org.springframework.context.SmartLifecycle;

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
public interface MqttClientManager<T extends MqttClient> extends SmartLifecycle {

	/**
	 * The default phase of this client manager auto-start in {@link SmartLifecycle}.
	 */
	int DEFAULT_MANAGER_PHASE = 0;

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
	 * @see MqttClientConnectedListener#onConnected(MqttClientConnectedContext)
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
