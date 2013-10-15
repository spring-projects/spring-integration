/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mqtt.inbound;

import java.util.concurrent.ScheduledFuture;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.Message;

/**
 * Eclipse Paho Implementation.
 *
 * @author Gary Russell
 * @since 1.0
 *
 */
public class MqttPahoMessageDrivenChannelAdapter extends AbstractMqttMessageDrivenChannelAdapter
		implements MqttCallback {

	private final MqttPahoClientFactory clientFactory;

	private volatile MqttClient client;

	private volatile ScheduledFuture<?> reconnectFuture;

	private volatile boolean connected;


	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, MqttPahoClientFactory clientFactory,
			String... topic) {
		super(url, clientId, topic);
		this.clientFactory = clientFactory;
	}

	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		this(url, clientId, new DefaultMqttPahoClientFactory(), topic);
	}

	@Override
	protected void doStart() {
		super.doStart();
		try {
			this.connectAndSubscribe();
		}
		catch (Exception e) {
			logger.error("Exception while connecting and subscribing, retrying", e);
			this.scheduleReconnect();
		}
	}

	@Override
	protected void doStop() {
		this.cancelReconnect();
		super.doStop();
		try {
			this.client.unsubscribe(this.getTopic());
		}
		catch (MqttException e) {
			logger.error("Exception while unsubscribing", e);
		}
		try {
			this.client.disconnect();
		}
		catch (MqttException e) {
			logger.error("Exception while disconnecting", e);
		}
		try {
			this.client.close();
		}
		catch (MqttException e) {
			logger.error("Exception while closing", e);
		}
		this.connected = false;
		this.client = null;
	}

	private void connectAndSubscribe() throws MqttException {
		this.client = this.clientFactory.getClientInstance(this.getUrl(), this.getClientId());
		this.client.connect(this.clientFactory.getConnectionOptions());
		try {
			this.client.subscribe(this.getTopic());
		}
		catch (MqttException e) {
			this.client.disconnect();
			throw e;
		}
		if (this.client.isConnected()) {
			this.client.setCallback(this);
			this.connected = true;
			if (this.reconnectFuture != null) {
				this.cancelReconnect();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Connected and subscribed to " + this.getTopic());
			}
		}
	}

	private synchronized void cancelReconnect() {
		if (this.reconnectFuture != null) {
			this.reconnectFuture.cancel(false);
			this.reconnectFuture = null;
		}
	}

	private void scheduleReconnect() {
		try {
			this.reconnectFuture = this.getTaskScheduler().scheduleWithFixedDelay(new Runnable() {

				@Override
				public void run() {
					try {
						if (logger.isDebugEnabled()) {
							logger.debug("Attempting reconnect");
						}
						if (!connected) {
							connectAndSubscribe();
						}
					}
					catch (MqttException e) {
						logger.error("Exception while connecting and subscribing", e);
					}
				}
			}, 10000);
		}
		catch (Exception e) {
			logger.error("Failed to schedule reconnect", e);
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		this.logger.error("Lost connection:" + cause.getMessage() + "; retrying...");
		this.connected = false;
		this.scheduleReconnect();
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
		Message<?> message = this.getConverter().toMessage(topic, mqttMessage);
		this.sendMessage(message);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

}
