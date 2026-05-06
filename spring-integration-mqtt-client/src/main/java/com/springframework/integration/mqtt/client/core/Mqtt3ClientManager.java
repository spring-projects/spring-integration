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

import com.hivemq.client.internal.mqtt.message.connect.mqtt3.Mqtt3ConnectView;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.springframework.integration.mqtt.client.event.MqttConnectionFailedEvent;
import com.springframework.integration.mqtt.client.support.MqttClientBuilderHelper;

import org.springframework.util.Assert;

/**
 * A client manager implementation for MQTT v3 protocol.
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class Mqtt3ClientManager extends AbstractMqttClientManager<Mqtt3Client, Mqtt3ClientBuilder>
		implements MqttClientConnectedListener {

	private Mqtt3Connect mqttConnect = Mqtt3ConnectView.DEFAULT;

	protected Mqtt3ClientManager(Mqtt3ClientBuilder mqttClientBuilder) {
		super(mqttClientBuilder);

		this.mqttClient = MqttClientBuilderHelper.clone(mqttClientBuilder)
				.addConnectedListener(Mqtt3ClientManager.this)
				.build();

		if (this.mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to enable 'automaticReconnect' when set the 'mqttClientBuilder'. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	/**
	 * Set the Connect message.
	 * @param mqttConnect the mqttConnect
	 */
	public void setMqttConnect(Mqtt3Connect mqttConnect) {
		Assert.notNull(mqttConnect, "'mqttConnect' must not be null.");
		this.mqttConnect = mqttConnect;
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
				logger.error("Could not start client manager", ex);
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
				this.mqttClient.toBlocking().disconnect();
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
