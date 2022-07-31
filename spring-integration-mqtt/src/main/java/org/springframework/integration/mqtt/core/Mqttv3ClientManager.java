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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.util.Assert;

/**
 * @author Artem Vozhdayenko
 * @since 6.0
 */
public class Mqttv3ClientManager extends AbstractMqttClientManager<IMqttAsyncClient> implements MqttCallback {

	private final MqttPahoClientFactory clientFactory;

	public Mqttv3ClientManager(String url, String clientId) {
		this(buildDefaultClientFactory(url), clientId);
	}

	public Mqttv3ClientManager(MqttPahoClientFactory clientFactory, String clientId) {
		super(clientId);
		Assert.notNull(clientFactory, "'clientFactory' is required");
		this.clientFactory = clientFactory;
		MqttConnectOptions connectionOptions = clientFactory.getConnectionOptions();
		String[] serverURIs = connectionOptions.getServerURIs();
		Assert.notEmpty(serverURIs, "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		setUrl(serverURIs[0]);
		if (!connectionOptions.isAutomaticReconnect()) {
			logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to set 'automaticReconnect' MQTT connection option. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	private static MqttPahoClientFactory buildDefaultClientFactory(String url) {
		Assert.notNull(url, "'url' is required");
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setServerURIs(new String[]{ url });
		connectOptions.setAutomaticReconnect(true);
		DefaultMqttPahoClientFactory defaultFactory = new DefaultMqttPahoClientFactory();
		defaultFactory.setConnectionOptions(connectOptions);
		return defaultFactory;
	}

	@Override
	public synchronized void start() {
		if (this.client == null) {
			try {
				this.client = this.clientFactory.getAsyncClientInstance(getUrl(), getClientId());
				this.client.setManualAcks(isManualAcks());
				this.client.setCallback(this);
			}
			catch (MqttException e) {
				throw new IllegalStateException("could not start client manager", e);
			}
		}
		try {
			MqttConnectOptions options = this.clientFactory.getConnectionOptions();
			this.client.connect(options).waitForCompletion(options.getConnectionTimeout());
		}
		catch (MqttException e) {
			logger.error("could not start client manager, client_id=" + this.client.getClientId(), e);

			if (this.clientFactory.getConnectionOptions().isAutomaticReconnect()) {
				try {
					this.client.reconnect();
				}
				catch (MqttException ex) {
					logger.error("MQTT client failed to re-connect.", ex);
				}
			}
			else if (getApplicationEventPublisher() != null) {
				getApplicationEventPublisher().publishEvent(new MqttConnectionFailedEvent(this, e));
			}
		}
	}

	@Override
	public synchronized void stop() {
		if (this.client == null) {
			return;
		}
		try {
			this.client.disconnectForcibly(this.clientFactory.getConnectionOptions().getConnectionTimeout());
		}
		catch (MqttException e) {
			logger.error("could not disconnect from the client", e);
		}
		finally {
			try {
				this.client.close();
			}
			catch (MqttException e) {
				logger.error("could not close the client", e);
			}
			this.client = null;
		}
	}

	@Override
	public synchronized void connectionLost(Throwable cause) {
		logger.error("connection lost, client_id=" + this.client.getClientId(), cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		// not this manager concern
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nor this manager concern
	}

}
