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

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.management.ManageableLifecycle;

public class Mqttv3ClientManager extends IntegrationObjectSupport implements ClientManager<IMqttAsyncClient>,
		ManageableLifecycle, MqttCallback {

	private AtomicReference<ScheduledFuture<?>> scheduledReconnect;

	private final MqttConnectOptions connectOptions;

	private final String clientId;

	private IMqttAsyncClient client;

	public Mqttv3ClientManager(MqttConnectOptions connectOptions, String clientId) throws MqttException {
		this.connectOptions = connectOptions;
		this.client = new MqttAsyncClient(connectOptions.getServerURIs()[0], clientId);
		this.client.setCallback(this);
		this.clientId = clientId;
	}

	@Override
	public IMqttAsyncClient getClient() {
		return client;
	}

	@Override
	public void start() {
		if (this.client == null) {
			try {
				this.client = new MqttAsyncClient(this.connectOptions.getServerURIs()[0], this.clientId);
			}
			catch (MqttException e) {
				throw new IllegalStateException("could not start client manager", e);
			}
			this.client.setCallback(this);
		}
		try {
			connect();
		}
		catch (MqttException e) {
			logger.error(e, "could not start client manager, scheduling reconnect, client_id=" +
					this.client.getClientId());
			scheduleReconnect();
		}
	}

	@Override
	public void stop() {
		if (this.client == null) {
			return;
		}
		try {
			this.client.disconnectForcibly(this.connectOptions.getConnectionTimeout());
		}
		catch (MqttException e) {
			logger.error(e, "could not disconnect from the client");
		}
		finally {
			try {
				this.client.close();
			}
			catch (MqttException e) {
				logger.error(e, "could not close the client");
			}
			this.client = null;
		}
	}

	@Override
	public boolean isRunning() {
		return this.client != null;
	}

	private synchronized void connect() throws MqttException {
		if (this.client == null) {
			logger.error("could not connect on a null client reference");
			return;
		}
		MqttConnectOptions options = Mqttv3ClientManager.this.connectOptions;
		this.client.connect(options).waitForCompletion(options.getConnectionTimeout());
	}

	@Override
	public synchronized void connectionLost(Throwable cause) {
		logger.error(cause, "connection lost, scheduling reconnect, client_id=" + this.client.getClientId());
		scheduleReconnect();
	}

	private void scheduleReconnect() {
		if (this.scheduledReconnect.get() != null) {
			this.scheduledReconnect.get().cancel(false);
		}
		this.scheduledReconnect.set(getTaskScheduler().schedule(() -> {
			try {
				connect();
				this.scheduledReconnect.set(null);
			}
			catch (MqttException e) {
				logger.error(e, "could not reconnect");
				scheduleReconnect();
			}
		}, Instant.now().plusSeconds(10)));
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
