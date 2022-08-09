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

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.util.Assert;

/**
 * @author Artem Vozhdayenko
 * @since 6.0
 */
public class Mqttv5ClientManager extends AbstractMqttClientManager<IMqttAsyncClient, MqttConnectionOptions>
		implements MqttCallback {

	private final MqttConnectionOptions connectionOptions;

	public Mqttv5ClientManager(String url, String clientId) {
		this(buildDefaultConnectionOptions(url), clientId);
	}

	public Mqttv5ClientManager(MqttConnectionOptions connectionOptions, String clientId) {
		super(clientId);
		Assert.notNull(connectionOptions, "'connectionOptions' is required");
		this.connectionOptions = connectionOptions;
		if (!this.connectionOptions.isAutomaticReconnect()) {
			logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to set 'automaticReconnect' MQTT connection option. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
		Assert.notEmpty(connectionOptions.getServerURIs(), "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		setUrl(connectionOptions.getServerURIs()[0]);
	}

	private static MqttConnectionOptions buildDefaultConnectionOptions(String url) {
		Assert.notNull(url, "'url' is required");
		var connectionOptions = new MqttConnectionOptions();
		connectionOptions.setServerURIs(new String[]{ url });
		connectionOptions.setAutomaticReconnect(true);
		return connectionOptions;
	}

	@Override
	public synchronized void start() {
		if (getClient() == null) {
			try {
				var client = new MqttAsyncClient(getUrl(), getClientId());
				client.setManualAcks(isManualAcks());
				client.setCallback(this);
				setClient(client);
			}
			catch (MqttException e) {
				throw new IllegalStateException("could not start client manager", e);
			}
		}
		try {
			getClient().connect(this.connectionOptions)
					.waitForCompletion(this.connectionOptions.getConnectionTimeout());
		}
		catch (MqttException e) {
			logger.error("could not start client manager, client_id=" + getClientId(), e);

			if (getConnectionInfo().isAutomaticReconnect()) {
				try {
					getClient().reconnect();
				}
				catch (MqttException re) {
					logger.error("MQTT client failed to connect. Never happens.", re);
				}
			}
			else {
				var applicationEventPublisher = getApplicationEventPublisher();
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, e));
				}
			}
		}
	}

	@Override
	public synchronized void stop() {
		var client = getClient();
		if (client == null) {
			return;
		}

		try {
			client.disconnectForcibly(this.connectionOptions.getConnectionTimeout());
		}
		catch (MqttException e) {
			logger.error("could not disconnect from the client", e);
		}
		finally {
			try {
				client.close();
			}
			catch (MqttException e) {
				logger.error("could not close the client", e);
			}
			setClient(null);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		// not this manager concern
	}

	@Override
	public void deliveryComplete(IMqttToken token) {
		// not this manager concern
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		getCallbacks().forEach(callback -> callback.connectComplete(reconnect));
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// not this manager concern
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		if (logger.isInfoEnabled()) {
			logger.info("MQTT disconnected: " + disconnectResponse);
		}
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		logger.error("MQTT error occurred", exception);
	}

	@Override
	public MqttConnectionOptions getConnectionInfo() {
		return this.connectionOptions;
	}
}
