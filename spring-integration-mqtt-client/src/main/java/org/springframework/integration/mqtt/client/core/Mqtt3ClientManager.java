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

package org.springframework.integration.mqtt.client.core;

import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientConfig;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;

import org.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;

/**
 * A client manager implementation for MQTT v3 protocol.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt3ClientManager extends AbstractMqttClientManager<Mqtt3Client, Mqtt3ClientConfig>
		implements MqttClientConnectedListener {

	private Mqtt3Connect mqttConnect = Mqtt3Connect.builder().build();

	public Mqtt3ClientManager(Mqtt3ClientConfig mqtt3ClientConfig) {
		super(mqtt3ClientConfig);
	}

	@Override
	protected Mqtt3Client buildClient(Mqtt3ClientConfig mqttClientConfig) {
		return createBaseClientBuilder(mqttClientConfig)
				.useMqttVersion3()
				.willPublish(mqttClientConfig.getWillPublish().orElse(null))
				.simpleAuth(mqttClientConfig.getSimpleAuth().orElse(null))
				.addConnectedListener(this)
				.build();
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(Mqtt3Connect mqttConnect) {
		this.mqttConnect = mqttConnect;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!this.isConnected()) {
				this.mqttClient.toBlocking().connect(this.mqttConnect);
			}
		}
		catch (RuntimeException ex) {
			this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
			logger.error(ex, "Could not start client manager");
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (this.isConnected()) {
				this.mqttClient.toBlocking().disconnect();
			}
		}
		catch (RuntimeException ex) {
			logger.error(ex, "Could not disconnect from the client");
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void onConnected(MqttClientConnectedContext context) {
		this.connectCallbacks.forEach(connectCallback -> connectCallback.onClientConnected(context));
	}

}
