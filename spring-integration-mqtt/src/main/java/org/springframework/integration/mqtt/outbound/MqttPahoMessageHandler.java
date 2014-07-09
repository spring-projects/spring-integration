/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.mqtt.outbound;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Eclipse Paho implementation.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MqttPahoMessageHandler extends AbstractMqttMessageHandler
		implements MqttCallback {

	private final MqttPahoClientFactory clientFactory;

	private volatile MqttClient client;

	/**
	 * Use this constructor for a single url (although it may be overridden
	 * if the server URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()}
	 * provided by the {@link MqttPahoClientFactory}).
	 * @param url the URL.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 */
	public MqttPahoMessageHandler(String url, String clientId, MqttPahoClientFactory clientFactory) {
		super(url, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor if the server URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()}
	 * provided by the {@link MqttPahoClientFactory}.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 * @since 4.1
	 */
	public MqttPahoMessageHandler(String clientId, MqttPahoClientFactory clientFactory) {
		super(null, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this URL when you don't need additional {@link MqttConnectOptions}.
	 * @param url The URL.
	 * @param clientId The client id.
	 */
	public MqttPahoMessageHandler(String url, String clientId) {
		this(url, clientId, new DefaultMqttPahoClientFactory());
	}

	@Override
	protected void doStart() {
	}

	@Override
	protected void doStop() {
		try {
			if (this.client != null) {
				this.client.disconnect();
				this.client.close();
				this.client = null;
			}
		}
		catch (MqttException e) {
			logger.error("Failed to disconnect", e);
		}
	}

	private synchronized void doConnect() throws MqttException {
		if (this.client != null && !this.client.isConnected()) {
			this.client.close();
			this.client = null;
		}
		if (this.client == null) {
			this.client = this.clientFactory.getClientInstance(this.getUrl(), this.getClientId());
			MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
			Assert.state(this.getUrl() != null || connectionOptions.getServerURIs() != null,
					"If no 'url' provided, connectionOptions.getServerURIs() must not be null");
			this.client.connect(connectionOptions);
			this.client.setCallback(this);
			if (logger.isDebugEnabled()) {
				logger.debug("Client connected");
			}
		}
	}

	@Override
	protected void connectIfNeeded() {
		if (this.client == null || !this.client.isConnected()) {
			try {
				this.doConnect();
			}
			catch (MqttException e) {
				throw new MessagingException("Failed to connect", e);
			}
		}
	}

	@Override
	protected void publish(String topic, Object mqttMessage) throws Exception {
		Assert.isInstanceOf(MqttMessage.class, mqttMessage);
		this.client.publish(topic, (MqttMessage) mqttMessage);
	}

	@Override
	public void connectionLost(Throwable cause) {
		logger.error("Lost connection; will attempt reconnect on next request");
		this.client = null;
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

	}

}
