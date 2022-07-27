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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.springframework.util.Assert;

public class Mqttv5ClientManager extends AbstractMqttClientManager<IMqttAsyncClient> implements MqttCallback {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MqttConnectionOptions connectionOptions;

	private volatile IMqttAsyncClient client;

	public Mqttv5ClientManager(MqttConnectionOptions connectionOptions, String url, String clientId) {
		super(url, clientId);
		Assert.notNull(connectionOptions, "'connectionOptions' is required");
		if (url == null) {
			Assert.notEmpty(connectionOptions.getServerURIs(), "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		}
		this.connectionOptions = connectionOptions;
	}

	@Override
	public IMqttAsyncClient getClient() {
		return this.client;
	}

	@Override
	public synchronized void start() {
		if (this.client == null) {
			try {
				this.client = new MqttAsyncClient(getUrl(), getClientId());
				this.client.setManualAcks(isManualAcks());
				this.client.setCallback(this);
			}
			catch (MqttException e) {
				throw new IllegalStateException("could not start client manager", e);
			}
		}
		try {
			this.client.connect(this.connectionOptions)
					.waitForCompletion(this.connectionOptions.getConnectionTimeout());
		}
		catch (MqttException e) {
			this.logger.error("could not start client manager, client_id=" + this.client.getClientId(), e);
		}
	}

	@Override
	public synchronized void stop() {
		if (this.client == null) {
			return;
		}

		try {
			this.client.disconnectForcibly(this.connectionOptions.getConnectionTimeout());
		}
		catch (MqttException e) {
			this.logger.error("could not disconnect from the client", e);
		}
		finally {
			try {
				this.client.close();
			}
			catch (MqttException e) {
				this.logger.error("could not close the client", e);
			}
			this.client = null;
		}
	}

	@Override
	public synchronized boolean isRunning() {
		return this.client != null;
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
		if (this.logger.isInfoEnabled()) {
			this.logger.info("MQTT connect complete to " + serverURI);
		}
		// probably makes sense to use custom callbacks in the future
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// not this manager concern
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("MQTT disconnected" + disconnectResponse);
		}
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		this.logger.error("MQTT error occurred", exception);
	}

}
