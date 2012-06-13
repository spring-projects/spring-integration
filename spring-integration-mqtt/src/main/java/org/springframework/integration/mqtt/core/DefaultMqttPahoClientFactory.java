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
package org.springframework.integration.mqtt.core;

import java.util.Properties;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Creates a default {@link MqttClient} and a set of options as configured.
 * @author Gary Russell
 * @since 1.0
 *
 */
public class DefaultMqttPahoClientFactory implements MqttPahoClientFactory {

	private volatile Boolean cleanSession;

	private volatile Integer connectionTimeout;

	private volatile Integer keepAliveInterval;

	private volatile String password;

	private volatile SocketFactory socketFactory;

	private volatile Properties sslProperties;

	private volatile String userName;

	private volatile MqttClientPersistence persistence;

	private volatile Will will;

	public void setCleanSession(Boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public void setKeepAliveInterval(Integer keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public void setSslProperties(Properties sslProperties) {
		this.sslProperties = sslProperties;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setWill(Will will) {
		this.will = will;
	}

	public void setPersistence(MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public MqttClient getClientInstance(String url, String clientId) throws MqttException {
		return new MqttClient(url, clientId, this.persistence);
	}

	@Override
	public MqttConnectOptions getConnectionOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		if (this.cleanSession != null) {
			options.setCleanSession(this.cleanSession);
		}
		if (this.connectionTimeout != null) {
			options.setConnectionTimeout(this.connectionTimeout);
		}
		if (this.keepAliveInterval != null) {
			options.setKeepAliveInterval(this.keepAliveInterval);
		}
		if (this.password != null) {
			options.setPassword(this.password.toCharArray());
		}
		if (this.socketFactory != null) {
			options.setSocketFactory(this.socketFactory);
		}
		if (this.sslProperties != null) {
			options.setSSLProperties(this.sslProperties);
		}
		if (this.userName != null) {
			options.setUserName(this.userName);
		}
		if (this.will != null) {
			options.setWill(this.will.getTopic(), this.will.getPayload(), this.will.getQos(), this.will.isRetained());
		}
		return options;
	}

	public static class Will {

		private final String topic;

		private final byte[] payload;

		private final int qos;

		private final boolean retained;

		public Will(String topic, byte[] payload, int qos, boolean retained) {
			this.topic = topic;
			this.payload = payload;
			this.qos = qos;
			this.retained = retained;
		}

		protected String getTopic() {
			return topic;
		}

		protected byte[] getPayload() {
			return payload;
		}

		protected int getQos() {
			return qos;
		}

		protected boolean isRetained() {
			return retained;
		}

	}

}
