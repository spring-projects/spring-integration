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

import com.hivemq.client.internal.mqtt.message.connect.MqttConnect;
import com.hivemq.client.internal.mqtt.message.disconnect.MqttDisconnect;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect;
import com.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import com.springframework.integration.mqtt.client.support.MqttClientBuilderHelper;

import org.springframework.util.Assert;

/**
 * A client manager implementation for MQTT v5 protocol.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt5ClientManager extends AbstractMqttClientManager<Mqtt5Client, Mqtt5ClientBuilder>
		implements MqttClientConnectedListener {

	private Mqtt5Connect mqttConnect = MqttConnect.DEFAULT;

	private Mqtt5Disconnect mqttDisConnect = MqttDisconnect.DEFAULT;

	@SuppressWarnings("this-escape")
	public Mqtt5ClientManager(Mqtt5ClientBuilder mqttClientBuilder) {
		super(mqttClientBuilder);

		this.mqttClient = MqttClientBuilderHelper.clone(mqttClientBuilder)
				.addConnectedListener(Mqtt5ClientManager.this)
				.build();

		if (this.mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.info("If this `MqttClientManager` is used from message-driven channel adapters, " +
					"it is recommended to enable 'automaticReconnect' when set the 'mqttClientBuilder'. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(Mqtt5Connect mqttConnect) {
		Assert.notNull(mqttConnect, "'mqttConnect' must not be null.");
		this.mqttConnect = mqttConnect;
	}

	/**
	 * Set the Disconnect message.
	 * @param mqttDisconnect the mqttDisconnect
	 */
	public void setMqttDisconnect(Mqtt5Disconnect mqttDisconnect) {
		Assert.notNull(mqttDisconnect, "'mqttDisconnect' must not be null.");
		this.mqttDisConnect = mqttDisconnect;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			try {
				this.mqttClient.toBlocking().connect(this.mqttConnect);
			}
			catch (RuntimeException ex) {
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
				logger.error("Could not start client manager.", ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			try {
				this.mqttClient.toBlocking().disconnect(this.mqttDisConnect);
			}
			catch (RuntimeException ex) {
				logger.error("Could not disconnect from the client", ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void onConnected(MqttClientConnectedContext context) {
		connectCallbacks.forEach(connectCallback -> connectCallback.onClientConnected(context));
	}

}
