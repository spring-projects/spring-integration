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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.util.Assert;

/**
 * A client manager implementation for MQTT v3 protocol. Requires a client ID and server URI.
 * If needed, the connection options may be overridden and passed as a {@link MqttConnectOptions} dependency.
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
public class Mqttv3ClientManager
		extends AbstractMqttClientManager<IMqttAsyncClient, MqttConnectOptions>
		implements MqttCallbackExtended {

	private final MqttConnectOptions connectionOptions;

	private MqttClientPersistence persistence;

	public Mqttv3ClientManager(String url, String clientId) {
		this(buildDefaultConnectionOptions(url), clientId);
	}

	@SuppressWarnings("this-escape")
	public Mqttv3ClientManager(MqttConnectOptions connectionOptions, String clientId) {
		super(clientId);
		Assert.notNull(connectionOptions, "'connectionOptions' is required");
		this.connectionOptions = connectionOptions;
		String[] serverURIs = connectionOptions.getServerURIs();
		Assert.notEmpty(serverURIs, "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		setUrl(serverURIs[0]);
		if (!connectionOptions.isAutomaticReconnect()) {
			logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to set 'automaticReconnect' MQTT connection option. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	private static MqttConnectOptions buildDefaultConnectionOptions(String url) {
		Assert.notNull(url, "'url' is required");
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[] {url});
		connectOptions.setAutomaticReconnect(true);
		return connectOptions;
	}

	/**
	 * Set the {@link MqttClientPersistence} for a client.
	 * @param persistence persistence implementation to use for te client
	 */
	public void setPersistence(MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public MqttConnectOptions getConnectionInfo() {
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
					throw new IllegalStateException("could not start client manager", e);
				}
			}
			setClient(client);
			try {
				client.connect(this.connectionOptions).waitForCompletion(getCompletionTimeout());
			}
			catch (MqttException ex) {
				// See GH-3822
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

	private IMqttAsyncClient createClient() throws MqttException {
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
				client.disconnectForcibly(getQuiescentTimeout(), getDisconnectCompletionTimeout());
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
	public void connectionLost(Throwable cause) {
		this.lock.lock();
		try {
			logger.error("Connection lost, client_id=" + getClientId(), cause);
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		getCallbacks().forEach(callback -> callback.connectComplete(reconnect));
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		// not this manager concern
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nor this manager concern
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
