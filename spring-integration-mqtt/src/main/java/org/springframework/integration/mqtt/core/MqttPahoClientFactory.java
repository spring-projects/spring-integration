/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mqtt.core;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
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
	MqttClient getClientInstance(String url, String clientId) throws MqttException;

	/**
	 * Retrieve an async client instance.
	 *
	 * @param url The URL.
	 * @param clientId The client id.
	 * @return The client instance.
	 * @throws MqttException Any.
	 * @since 4.1
	 */
	MqttAsyncClient getAsyncClientInstance(String url, String clientId) throws MqttException;

	/**
	 * Retrieve the connection options.
	 *
	 * @return The options.
	 */
	MqttConnectOptions getConnectionOptions();
}
