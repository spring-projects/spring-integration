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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

public class Mqttv3ClientManager extends AbstractMqttClientManager<IMqttAsyncClient> implements MqttCallback {

	/**
	 * The default reconnect timeout in millis.
	 */
	private static final long DEFAULT_RECOVERY_INTERVAL = 10_000;

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MqttPahoClientFactory clientFactory;

	private final TaskScheduler taskScheduler;

	private volatile ScheduledFuture<?> scheduledReconnect;

	private volatile IMqttAsyncClient client;

	private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	public Mqttv3ClientManager(MqttPahoClientFactory clientFactory, TaskScheduler taskScheduler, String url,
			String clientId) {

		super(url, clientId);
		Assert.notNull(clientId, "'clientFactory' is required");
		Assert.notNull(clientId, "'taskScheduler' is required");
		if (url == null) {
			Assert.notEmpty(clientFactory.getConnectionOptions().getServerURIs(), "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		}
		this.clientFactory = clientFactory;
		this.taskScheduler = taskScheduler;
	}

	@Override
	public IMqttAsyncClient getClient() {
		return this.client;
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
			connect();
		}
		catch (MqttException e) {
			this.logger.error("could not start client manager, scheduling reconnect, client_id=" +
					this.client.getClientId(), e);
			scheduleReconnect();
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
	public synchronized void connectionLost(Throwable cause) {
		this.logger.error("connection lost, scheduling reconnect, client_id=" + this.client.getClientId(),
				cause);
		scheduleReconnect(); // todo: do we need to resubscribe if con lost?
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		// not this manager concern
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// nor this manager concern
	}

	public long getRecoveryInterval() {
		return this.recoveryInterval;
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	private synchronized void connect() throws MqttException {
		MqttConnectOptions options = this.clientFactory.getConnectionOptions();
		this.client.connect(options).waitForCompletion(options.getConnectionTimeout());
	}

	private synchronized void scheduleReconnect() {
		if (this.scheduledReconnect != null) {
			this.scheduledReconnect.cancel(false);
		}
		this.scheduledReconnect = this.taskScheduler.schedule(() -> {
			try {
				if (this.client.isConnected()) {
					return;
				}

				connect();
				this.scheduledReconnect = null;
			}
			catch (MqttException e) {
				this.logger.error("could not reconnect", e);
				scheduleReconnect();
			}
		}, Instant.now().plusMillis(getRecoveryInterval()));
	}

}
