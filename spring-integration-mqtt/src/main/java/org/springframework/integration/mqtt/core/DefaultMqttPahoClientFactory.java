/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.List;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import org.springframework.util.Assert;

/**
 * Creates a default {@link MqttClient} and a set of options as configured.
 *
 * @author Gary Russell
 * @author Gunnar Hillert
 *
 * @since 4.0
 *
 */
public class DefaultMqttPahoClientFactory implements MqttPahoClientFactory {

	private MqttConnectOptions options = new MqttConnectOptions();

	private MqttClientPersistence persistence;

	private ConsumerStopAction consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;

	private List<String> serverUris;

	/**
	 * Set the persistence to pass into the client constructor.
	 * @param persistence the persistence to set.
	 */
	public void setPersistence(MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	/**
	 * Get the consumer stop action.
	 * @return the consumer stop action.
	 * @since 4.2.3
	 */
	@Override
	public ConsumerStopAction getConsumerStopAction() {
		return this.consumerStopAction;
	}

	/**
	 * Set the consumer stop action. Determines whether we unsubscribe when the consumer stops.
	 * Default: {@link ConsumerStopAction#UNSUBSCRIBE_CLEAN}.
	 * @param consumerStopAction the consumer stop action.
	 * @since 4.2.3.
	 */
	public void setConsumerStopAction(ConsumerStopAction consumerStopAction) {
		this.consumerStopAction = consumerStopAction;
	}

	@Override
	public IMqttClient getClientInstance(String uri, String clientId) throws MqttException {
		// Client validates URI even if overridden by options
		return new MqttClient(uri == null ? "tcp://NO_URL_PROVIDED" : uri, clientId, this.persistence);
	}

	@Override
	public IMqttAsyncClient getAsyncClientInstance(String uri, String clientId) throws MqttException {
		// Client validates URI even if overridden by options
		return new MqttAsyncClient(uri == null ? "tcp://NO_URL_PROVIDED" : uri, clientId, this.persistence);
	}

	/**
	 * Set the preconfigured {@link MqttConnectOptions}.
	 * @param options the options.
	 * @since 4.3.16
	 */
	public void setConnectionOptions(MqttConnectOptions options) {
		Assert.notNull(options, "MqttConnectOptions cannot be null");
		this.options = options;
	}

	@Override
	public MqttConnectOptions getConnectionOptions() {
		if (this.serverUris == null) {
			return this.options;
		}
		else {
			return new ExtendedOptions();
		}
	}

	@Override
	public void setServerUris(List<String> serverUris) {
		this.serverUris = serverUris;
	}

	private class ExtendedOptions extends MqttConnectOptions {

		@Override
		public char[] getPassword() {
			return DefaultMqttPahoClientFactory.this.options.getPassword();
		}

		@Override
		public void setPassword(char[] password) {
			DefaultMqttPahoClientFactory.this.options.setPassword(password);
		}

		@Override
		public String getUserName() {
			return DefaultMqttPahoClientFactory.this.options.getUserName();
		}

		@Override
		public void setUserName(String userName) {
			DefaultMqttPahoClientFactory.this.options.setUserName(userName);
		}

		@Override
		public int getMaxReconnectDelay() {
			return DefaultMqttPahoClientFactory.this.options.getMaxReconnectDelay();
		}

		@Override
		public void setMaxReconnectDelay(int maxReconnectDelay) {
			DefaultMqttPahoClientFactory.this.options.setMaxReconnectDelay(maxReconnectDelay);
		}

		@Override
		public void setWill(MqttTopic topic, byte[] payload, int qos, boolean retained) {
			DefaultMqttPahoClientFactory.this.options.setWill(topic, payload, qos, retained);
		}

		@Override
		public void setWill(String topic, byte[] payload, int qos, boolean retained) {
			DefaultMqttPahoClientFactory.this.options.setWill(topic, payload, qos, retained);
		}

		@Override
		public int getKeepAliveInterval() {
			return DefaultMqttPahoClientFactory.this.options.getKeepAliveInterval();
		}

		@Override
		public int getMqttVersion() {
			return DefaultMqttPahoClientFactory.this.options.getMqttVersion();
		}

		@Override
		public void setKeepAliveInterval(int keepAliveInterval) throws IllegalArgumentException {
			DefaultMqttPahoClientFactory.this.options.setKeepAliveInterval(keepAliveInterval);
		}

		@Override
		public int getMaxInflight() {
			return DefaultMqttPahoClientFactory.this.options.getMaxInflight();
		}

		@Override
		public void setMaxInflight(int maxInflight) {
			DefaultMqttPahoClientFactory.this.options.setMaxInflight(maxInflight);
		}

		@Override
		public int getConnectionTimeout() {
			return DefaultMqttPahoClientFactory.this.options.getConnectionTimeout();
		}

		@Override
		public void setConnectionTimeout(int connectionTimeout) {
			DefaultMqttPahoClientFactory.this.options.setConnectionTimeout(connectionTimeout);
		}

		@Override
		public SocketFactory getSocketFactory() {
			return DefaultMqttPahoClientFactory.this.options.getSocketFactory();
		}

		@Override
		public void setSocketFactory(SocketFactory socketFactory) {
			DefaultMqttPahoClientFactory.this.options.setSocketFactory(socketFactory);
		}

		@Override
		public String getWillDestination() {
			return DefaultMqttPahoClientFactory.this.options.getWillDestination();
		}

		@Override
		public MqttMessage getWillMessage() {
			return DefaultMqttPahoClientFactory.this.options.getWillMessage();
		}

		@Override
		public Properties getSSLProperties() {
			return DefaultMqttPahoClientFactory.this.options.getSSLProperties();
		}

		@Override
		public void setSSLProperties(Properties props) {
			DefaultMqttPahoClientFactory.this.options.setSSLProperties(props);
		}

		@Override
		public boolean isHttpsHostnameVerificationEnabled() {
			return DefaultMqttPahoClientFactory.this.options.isHttpsHostnameVerificationEnabled();
		}

		@Override
		public void setHttpsHostnameVerificationEnabled(boolean httpsHostnameVerificationEnabled) {
			DefaultMqttPahoClientFactory.this.options.setHttpsHostnameVerificationEnabled(httpsHostnameVerificationEnabled);
		}

		@Override
		public HostnameVerifier getSSLHostnameVerifier() {
			return DefaultMqttPahoClientFactory.this.options.getSSLHostnameVerifier();
		}

		@Override
		public void setSSLHostnameVerifier(HostnameVerifier hostnameVerifier) {
			DefaultMqttPahoClientFactory.this.options.setSSLHostnameVerifier(hostnameVerifier);
		}

		@Override
		public boolean isCleanSession() {
			return DefaultMqttPahoClientFactory.this.options.isCleanSession();
		}

		@Override
		public void setCleanSession(boolean cleanSession) {
			DefaultMqttPahoClientFactory.this.options.setCleanSession(cleanSession);
		}

		@Override
		public String[] getServerURIs() {
			if (DefaultMqttPahoClientFactory.this.serverUris != null) {
				return DefaultMqttPahoClientFactory.this.serverUris.toArray(new String[0]);
			}
			else {
				return DefaultMqttPahoClientFactory.this.options.getServerURIs();
			}
		}

		@Override
		public void setServerURIs(String[] serverURIs) {
			DefaultMqttPahoClientFactory.this.options.setServerURIs(serverURIs);
		}

		@Override
		public void setMqttVersion(int mqttVersion) throws IllegalArgumentException {
			DefaultMqttPahoClientFactory.this.options.setMqttVersion(mqttVersion);
		}

		@Override
		public boolean isAutomaticReconnect() {
			return DefaultMqttPahoClientFactory.this.options.isAutomaticReconnect();
		}

		@Override
		public void setAutomaticReconnect(boolean automaticReconnect) {
			DefaultMqttPahoClientFactory.this.options.setAutomaticReconnect(automaticReconnect);
		}

		@Override
		public int getExecutorServiceTimeout() {
			return DefaultMqttPahoClientFactory.this.options.getExecutorServiceTimeout();
		}

		@Override
		public void setExecutorServiceTimeout(int executorServiceTimeout) {
			DefaultMqttPahoClientFactory.this.options.setExecutorServiceTimeout(executorServiceTimeout);
		}

		@Override
		public Properties getDebug() {
			return DefaultMqttPahoClientFactory.this.options.getDebug();
		}

		@Override
		public void setCustomWebSocketHeaders(Properties props) {
			DefaultMqttPahoClientFactory.this.options.setCustomWebSocketHeaders(props);
		}

		@Override
		public Properties getCustomWebSocketHeaders() {
			return DefaultMqttPahoClientFactory.this.options.getCustomWebSocketHeaders();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((DefaultMqttPahoClientFactory.this.options == null)
					? 0
					: DefaultMqttPahoClientFactory.this.options.hashCode());
			if (DefaultMqttPahoClientFactory.this.serverUris != null) {
				result += DefaultMqttPahoClientFactory.this.serverUris.hashCode();
			}
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ExtendedOptions other = (ExtendedOptions) obj;
			if (DefaultMqttPahoClientFactory.this.options == null) {
				if (other.getEnclosingInstance().options != null) {
					return false;
				}
			}
			else if (!DefaultMqttPahoClientFactory.this.options.equals(other.getEnclosingInstance().options)) {
				return false;
			}
			return urisEqual(other.getEnclosingInstance().serverUris);
		}

		private boolean urisEqual(List<String> other) {
			if (other == null) {
				return false;
			}
			if (DefaultMqttPahoClientFactory.this.serverUris == null) {
				return false;
			}
			return DefaultMqttPahoClientFactory.this.serverUris.equals(other);
		}

		@Override
		public String toString() {
			return DefaultMqttPahoClientFactory.this.options.toString();
		}

		private DefaultMqttPahoClientFactory getEnclosingInstance() {
			return DefaultMqttPahoClientFactory.this;
		}

	}

	/**
	 * Deprecated.
	 * @deprecated - no longer used.
	 */
	@Deprecated
	public static class Will {

		private final String topic;

		private final byte[] payload;

		private final int qos;

		private final boolean retained;

		public Will(String topic, byte[] payload, int qos, boolean retained) { //NOSONAR
			this.topic = topic;
			this.payload = payload; //NOSONAR
			this.qos = qos;
			this.retained = retained;
		}

		protected String getTopic() {
			return this.topic;
		}

		protected byte[] getPayload() {
			return this.payload; //NOSONAR
		}

		protected int getQos() {
			return this.qos;
		}

		protected boolean isRetained() {
			return this.retained;
		}

	}

}
