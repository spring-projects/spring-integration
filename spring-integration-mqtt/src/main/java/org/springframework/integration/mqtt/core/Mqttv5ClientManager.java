/*
 * Copyright 2022-present the original author or authors.
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
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;

import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.util.Assert;

/**
 * A client manager implementation for MQTT v5 protocol. Requires a client ID and server URI.
 * If needed, the connection options may be overridden and passed as a {@link MqttConnectionOptions} dependency.
 * By default, automatic reconnect is used. If it is required to be turned off, one should listen for
 * {@link MqttConnectionFailedEvent} and reconnect the MQTT client manually.
 *
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Jiri Soucek
 *
 * @since 6.0
 */
public class Mqttv5ClientManager
		extends AbstractMqttClientManager<IMqttAsyncClient, MqttConnectionOptions>
		implements MqttCallback {

	private final MqttConnectionOptions connectionOptions;

	private MqttClientPersistence persistence;

	public Mqttv5ClientManager(String url, String clientId) {
		this(buildDefaultConnectionOptions(url), clientId);
	}

	@SuppressWarnings("this-escape")
	public Mqttv5ClientManager(MqttConnectionOptions connectionOptions, String clientId) {
		super(clientId);
		Assert.notNull(connectionOptions, "'connectionOptions' is required");
		this.connectionOptions = connectionOptions;
		if (!this.connectionOptions.isAutomaticReconnect()) {
			logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to set 'automaticReconnect' MQTT connection option. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
		Assert.notEmpty(connectionOptions.getServerURIs(),
				"'serverURIs' must be provided in the 'MqttConnectionOptions'");
		setUrl(connectionOptions.getServerURIs()[0]);
	}

	private static MqttConnectionOptions buildDefaultConnectionOptions(String url) {
		Assert.notNull(url, "'url' is required");
		var connectionOptions = new MqttConnectionOptions();
		connectionOptions.setServerURIs(new String[] {url});
		connectionOptions.setAutomaticReconnect(true);
		return connectionOptions;
	}

	/**
	 * Set the {@link org.eclipse.paho.client.mqttv3.MqttClientPersistence} for a client.
	 * @param persistence persistence implementation to use for te client
	 */
	public void setPersistence(MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public MqttConnectionOptions getConnectionInfo() {
		return this.connectionOptions;
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			var client = getClient();
			if (client == null) {
				try {
					client = createClient();
				}
				catch (MqttException e) {
					throw new IllegalStateException("Could not start client manager", e);
				}
			}
			setClient(client);
			try {
				client.connect(this.connectionOptions).waitForCompletion(getCompletionTimeout());
			}
			catch (MqttException ex) {
				if (this.connectionOptions.isAutomaticReconnect()) {
					try {
						client.reconnect();
					}
					catch (MqttException re) {
						logger.error("MQTT client failed to connect. Never happens.", re);
					}
				}
				else {
					var applicationEventPublisher = getApplicationEventPublisher();
					if (applicationEventPublisher != null) {
						applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
					}
					else {
						logger.error("Could not start client manager, client_id=" + getClientId(), ex);
					}
				}
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	private MqttAsyncClient createClient() throws MqttException {
		var url = getUrl();
		var clientId = getClientId();
		var client = new MqttAsyncClient(url, clientId, this.persistence);
		client.setManualAcks(isManualAcks());
		client.setCallback(this);
		return client;
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			var client = getClient();
			if (client == null) {
				return;
			}

			try {
				client.disconnectForcibly(getQuiescentTimeout(), getDisconnectCompletionTimeout(),
						MqttReturnCode.RETURN_CODE_SUCCESS, new MqttProperties());
				if (getConnectionInfo().isAutomaticReconnect()) {
					MqttUtils.stopClientReconnectCycle(client);
				}
			}
			catch (MqttException e) {
				logger.error("Could not disconnect from the client", e);
			}
			finally {
				try {
					client.close();
				}
				catch (MqttException e) {
					logger.error("Could not close the client", e);
				}
				setClient(null);
			}
		}
		finally {
			this.lock.unlock();
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
	public boolean isConnected() {
		this.lock.lock();
		try {
			IMqttAsyncClient client = getClient();
			if (client != null) {
				return client.isConnected();
			}
			return false;
		}
		finally {
			this.lock.unlock();
		}
	}
}
