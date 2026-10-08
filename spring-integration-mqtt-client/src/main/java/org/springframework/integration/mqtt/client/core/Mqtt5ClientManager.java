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
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConfig;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;

import org.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;

/**
 * A client manager implementation for MQTT v5 protocol.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5ClientManager extends AbstractMqttClientManager<Mqtt5Client, Mqtt5ClientConfig> {

	private Mqtt5Connect mqttConnect = Mqtt5Connect.builder().build();

	private Mqtt5Disconnect mqttDisConnect = Mqtt5Disconnect.builder().build();

	public Mqtt5ClientManager(Mqtt5ClientConfig mqtt5ClientConfig) {
		super(mqtt5ClientConfig);
	}

	@Override
	protected Mqtt5Client buildClient() {
		return createBaseClientBuilder(this.mqttClientConfig)
				.useMqttVersion5()
				.advancedConfig(this.mqttClientConfig.getAdvancedConfig())
				.willPublish(this.mqttClientConfig.getWillPublish().orElse(null))
				.simpleAuth(this.mqttClientConfig.getSimpleAuth().orElse(null))
				.enhancedAuth(this.mqttClientConfig.getEnhancedAuthMechanism().orElse(null))
				.addConnectedListener(this)
				.build();
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(Mqtt5Connect mqttConnect) {
		this.mqttConnect = mqttConnect;
	}

	/**
	 * Set the Disconnect message.
	 * @param mqttDisconnect the mqttDisconnect
	 */
	public void setMqttDisconnect(Mqtt5Disconnect mqttDisconnect) {
		this.mqttDisConnect = mqttDisconnect;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (!isConnected()) {
				getClient().toBlocking().connect(this.mqttConnect);
			}
		}
		catch (RuntimeException ex) {
			getApplicationEventPublisher().publishEvent(new MqttConnectionFailedEvent(this, ex));
			logger.error(ex, "Could not start client manager.");
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (isConnected()) {
				getClient().toBlocking().disconnect(this.mqttDisConnect);
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
