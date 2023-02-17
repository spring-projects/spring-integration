/*
 * Copyright 2002-2023 the original author or authors.
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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import org.springframework.util.Assert;

/**
 * Creates a default {@link MqttClient} and a set of options as configured.
 *
 * @author Gary Russell
 * @author Gunnar Hillert
 *
 * @since 4.0
 *
 */
public class DefaultMqttPahoClientFactory implements MqttPahoClientFactory {

	private MqttConnectOptions options = new MqttConnectOptions();

	private MqttClientPersistence persistence;

	/**
	 * Set the persistence to pass into the client constructor.
	 * @param persistence the persistence to set.
	 */
	public void setPersistence(MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public IMqttClient getClientInstance(String uri, String clientId) throws MqttException {
		// Client validates URI even if overridden by options
		return new MqttClient(uri == null ? "tcp://NO_URL_PROVIDED" : uri, clientId, this.persistence);
	}

	@Override
	public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
		// Client validates URI even if overridden by options
		return new MqttAsyncClient(uri == null ? "tcp://NO_URL_PROVIDED" : uri, clientId, this.persistence);
	}

	/**
	 * Set the preconfigured {@link MqttConnectOptions}.
	 * @param options the options.
	 * @since 4.3.16
	 */
	public void setConnectionOptions(MqttConnectOptions options) {
		Assert.notNull(options, "MqttConnectOptions cannot be null");
		this.options = options;
	}

	@Override
	public MqttConnectOptions getConnectionOptions() {
		return this.options;
	}

	public static class Will {

		private final String topic;

		private final byte[] payload;

		private final int qos;

		private final boolean retained;

		public Will(String topic, byte[] payload, int qos, boolean retained) { //NOSONAR
			this.topic = topic;
			this.payload = payload; //NOSONAR
			this.qos = qos;
			this.retained = retained;
		}

		protected String getTopic() {
			return this.topic;
		}

		protected byte[] getPayload() {
			return this.payload; //NOSONAR
		}

		protected int getQos() {
			return this.qos;
		}

		protected boolean isRetained() {
			return this.retained;
		}

	}

}
