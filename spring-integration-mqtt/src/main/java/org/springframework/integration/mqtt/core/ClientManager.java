/*
 * Copyright 2022-2022 the original author or authors.
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
 *
 * @since 6.0
 */
public interface ClientManager<T, C> extends SmartLifecycle, MqttComponent<C> {

	T getClient();

	boolean isManualAcks();

	void addCallback(ConnectCallback connectCallback);

	boolean removeCallback(ConnectCallback connectCallback);

	/**
	 * A contract for a custom callback if needed by a usage.
	 *
	 * @see org.eclipse.paho.mqttv5.client.MqttCallback#connectComplete
	 * @see org.eclipse.paho.client.mqttv3.MqttCallbackExtended#connectComplete
	 */
	interface ConnectCallback {

		void connectComplete(boolean isReconnect);

	}

}
