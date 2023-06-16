/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.mqtt.outbound;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Eclipse Paho Implementation. When consuming {@link org.springframework.integration.mqtt.event.MqttIntegrationEvent}s
 * published by this component use {@code MqttPahoComponent handler = event.getSourceAsType()} to get a
 * reference, allowing you to obtain the bean name and {@link MqttConnectOptions}. This
 * technique allows consumption of events from both inbound and outbound endpoints in the
 * same event listener.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Christian Tzolov
 *
 * @since 4.0
 *
 */
public class MqttPahoMessageHandler extends AbstractMqttMessageHandler<IMqttAsyncClient, MqttConnectOptions>
		implements MqttCallback, MqttPahoComponent {

	private final MqttPahoClientFactory clientFactory;

	private boolean async;

	private boolean asyncEvents;

	private volatile IMqttAsyncClient client;

	/**
	 * Use this constructor when you don't need additional {@link MqttConnectOptions}.
	 * @param url The URL.
	 * @param clientId The client id.
	 */
	public MqttPahoMessageHandler(String url, String clientId) {
		this(url, clientId, new DefaultMqttPahoClientFactory());
	}

	/**
	 * Use this constructor for a single url (although it may be overridden if the server
	 * URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()} provided by
	 * the {@link MqttPahoClientFactory}).
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
	 * Use this constructor when you need to use a single {@link ClientManager}
	 * (for instance, to reuse an MQTT connection).
	 * @param clientManager The client manager.
	 * @since 6.0
	 */
	public MqttPahoMessageHandler(ClientManager<IMqttAsyncClient, MqttConnectOptions> clientManager) {
		super(clientManager);
		var factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(clientManager.getConnectionInfo());
		this.clientFactory = factory;
	}

	/**
	 * Set to true if you don't want to block when sending messages. Default false.
	 * When true, message sent/delivered events will be published for reception
	 * by a suitably configured 'ApplicationListener' or an event
	 * inbound-channel-adapter.
	 * @param async true for async.
	 * @since 4.1
	 */
	public void setAsync(boolean async) {
		this.async = async;
	}

	/**
	 * When {@link #setAsync(boolean)} is true, setting this to true enables
	 * publication of {@link MqttMessageSentEvent} and {@link MqttMessageDeliveredEvent}
	 * to be emitted. Default false.
	 * @param asyncEvents the asyncEvents.
	 * @since 4.1
	 */
	public void setAsyncEvents(boolean asyncEvents) {
		this.asyncEvents = asyncEvents;
	}

	@Override
	public MqttConnectOptions getConnectionInfo() {
		MqttConnectOptions options = this.clientFactory.getConnectionOptions();
		if (options.getServerURIs() == null) {
			String url = getUrl();
			if (url != null) {
				options = MqttUtils.cloneConnectOptions(options);
				options.setServerURIs(new String[] {url});
			}
		}
		return options;
	}

	@Override
	protected void onInit() {
		super.onInit();
		MessageConverter converter = getConverter();
		if (converter == null) {
			DefaultPahoMessageConverter defaultConverter = new DefaultPahoMessageConverter(getDefaultQos(),
					getQosProcessor(), getDefaultRetained(), getRetainedProcessor());
			if (getBeanFactory() != null) {
				defaultConverter.setBeanFactory(getBeanFactory());
			}
			setConverter(defaultConverter);
		}
		else {
			Assert.state(converter instanceof MqttMessageConverter, "MessageConverter must be an MqttMessageConverter");
		}
	}

	@Override
	protected void doStart() {
	}

	@Override
	protected void doStop() {
		try {
			IMqttAsyncClient theClient = this.client;
			if (theClient != null) {
				theClient.disconnect().waitForCompletion(getDisconnectCompletionTimeout());
				theClient.close();
				this.client = null;
			}
		}
		catch (MqttException ex) {
			logger.error(ex, "Failed to disconnect");
		}
	}

	private IMqttAsyncClient checkConnection() throws MqttException {
		this.lock.lock();
		try {
			var theClientManager = getClientManager();
			if (theClientManager != null) {
				return theClientManager.getClient();
			}

			if (this.client != null && !this.client.isConnected()) {
				this.client.setCallback(null);
				this.client.close();
				this.client = null;
			}
			if (this.client == null) {
				try {
					MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
					Assert.state(this.getUrl() != null || connectionOptions.getServerURIs() != null,
							"If no 'url' provided, connectionOptions.getServerURIs() must not be null");
					this.client = this.clientFactory.getAsyncClientInstance(this.getUrl(), this.getClientId());
					incrementClientInstance();
					this.client.setCallback(this);
					this.client.connect(connectionOptions).waitForCompletion(getCompletionTimeout());
					logger.debug("Client connected");
				}
				catch (MqttException e) {
					if (this.client != null) {
						this.client.close();
						this.client = null;
					}
					ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
					if (applicationEventPublisher != null) {
						applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, e));
					}
					throw new MessagingException("Failed to connect", e);
				}
			}
			return this.client;
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	protected void publish(String topic, Object mqttMessage, Message<?> message) {
		Assert.isInstanceOf(MqttMessage.class, mqttMessage, "The 'mqttMessage' must be an instance of 'MqttMessage'");
		try {
			IMqttDeliveryToken token = checkConnection()
					.publish(topic, (MqttMessage) mqttMessage);
			ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
			if (!this.async) {
				token.waitForCompletion(getCompletionTimeout()); // NOSONAR (sync)
			}
			else if (this.asyncEvents && applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(
						new MqttMessageSentEvent(this, message, topic, token.getMessageId(), getClientId(),
								getClientInstance()));
			}
		}
		catch (MqttException e) {
			throw new MessageHandlingException(message, "Failed to publish to MQTT in the [" + this + ']', e);
		}
	}

	private void sendDeliveryComplete(IMqttDeliveryToken token) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (this.async && this.asyncEvents && applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(
					new MqttMessageDeliveredEvent(this, token.getMessageId(), getClientId(),
							getClientInstance()));
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		this.lock.lock();
		try {
			logger.error("Lost connection; will attempt reconnect on next request");
			if (this.client != null) {
				try {
					this.client.setCallback(null);
					this.client.close();
				}
				catch (MqttException e) {
					// NOSONAR
				}
				this.client = null;
				ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
				}
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		sendDeliveryComplete(token);
	}

}
