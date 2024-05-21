/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mqtt.inbound;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * Eclipse Paho Implementation. When consuming {@link org.springframework.integration.mqtt.event.MqttIntegrationEvent}s
 * published by this component use {@code MqttPahoComponent adapter = event.getSourceAsType()} to get a
 * reference, allowing you to obtain the bean name and {@link MqttConnectOptions}. This
 * technique allows consumption of events from both inbound and outbound endpoints in the
 * same event listener.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 *
 */
public class MqttPahoMessageDrivenChannelAdapter
		extends AbstractMqttMessageDrivenChannelAdapter<IMqttAsyncClient, MqttConnectOptions>
		implements MqttCallbackExtended, MqttPahoComponent {

	private final Lock lock = new ReentrantLock();

	private final MqttPahoClientFactory clientFactory;

	private volatile IMqttAsyncClient client;

	private volatile boolean readyToSubscribeOnStart;

	/**
	 * Use this constructor when you don't need additional {@link MqttConnectOptions}.
	 * @param url The URL.
	 * @param clientId The client id.
	 * @param topic The topic(s).
	 */
	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		this(url, clientId, new DefaultMqttPahoClientFactory(), topic);
	}

	/**
	 * Use this constructor for a single url (although it may be overridden if the server
	 * URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()} provided by
	 * the {@link MqttPahoClientFactory}).
	 * @param url the URL.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 * @param topic The topic(s).
	 */
	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, MqttPahoClientFactory clientFactory,
			String... topic) {

		super(url, clientId, topic);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor if the server URI(s) are provided by the
	 * {@link MqttConnectOptions#getServerURIs()} provided by the
	 * {@link MqttPahoClientFactory}.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 * @param topic The topic(s).
	 * @since 4.1
	 */
	public MqttPahoMessageDrivenChannelAdapter(String clientId, MqttPahoClientFactory clientFactory,
			String... topic) {

		super(null, clientId, topic);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor when you need to use a single {@link ClientManager}
	 * (for instance, to reuse an MQTT connection).
	 * @param clientManager The client manager.
	 * @param topic The topic(s).
	 * @since 6.0
	 */
	public MqttPahoMessageDrivenChannelAdapter(ClientManager<IMqttAsyncClient, MqttConnectOptions> clientManager,
			String... topic) {

		super(clientManager, topic);
		var factory = new DefaultMqttPahoClientFactory();
		factory.setConnectionOptions(clientManager.getConnectionInfo());
		this.clientFactory = factory;
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
		if (getConverter() == null) {
			DefaultPahoMessageConverter pahoMessageConverter = new DefaultPahoMessageConverter();
			pahoMessageConverter.setBeanFactory(getBeanFactory());
			setConverter(pahoMessageConverter);
		}
	}

	@Override
	protected void doStart() {
		try {
			connect();
			if (this.readyToSubscribeOnStart) {
				subscribe();
			}
		}
		catch (Exception ex) {
			if (getConnectionInfo().isAutomaticReconnect()) {
				try {
					this.client.reconnect();
				}
				catch (MqttException re) {
					logger.error(re, "MQTT client failed to connect. Never happens.");
				}
			}
			else {
				logger.error(ex, "Exception while connecting");
				var applicationEventPublisher = getApplicationEventPublisher();
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void connect() throws MqttException {
		this.lock.lock();
		try {
			MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
			var clientManager = getClientManager();
			if (clientManager == null) {
				Assert.state(getUrl() != null || connectionOptions.getServerURIs() != null,
						"If no 'url' provided, connectionOptions.getServerURIs() must not be null");
				this.client = this.clientFactory.getAsyncClientInstance(getUrl(), getClientId());
				this.client.setCallback(this);
				this.client.connect(connectionOptions).waitForCompletion(getCompletionTimeout());
				this.client.setManualAcks(isManualAcks());
			}
			else {
				this.client = clientManager.getClient();
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	protected void doStop() {
		this.lock.lock();
		try {
			this.readyToSubscribeOnStart = false;
			try {
				if (this.clientFactory.getConnectionOptions().isCleanSession()) {
					this.client.unsubscribe(getTopic());
					// Have to re-subscribe on next start if connection is not lost.
					this.readyToSubscribeOnStart = true;

				}
			}
			catch (MqttException ex1) {
				logger.error(ex1, "Exception while unsubscribing");
			}

			if (getClientManager() != null) {
				return;
			}

			try {
				this.client.disconnectForcibly(getDisconnectCompletionTimeout());
				if (getConnectionInfo().isAutomaticReconnect()) {
					MqttUtils.stopClientReconnectCycle(this.client);
				}
			}
			catch (MqttException ex) {
				logger.error(ex, "Exception while disconnecting");
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (getClientManager() == null) {
			try {
				this.client.close();
			}
			catch (MqttException e) {
				logger.error(e, "Could not close client");
			}
		}
	}

	@Override
	public void addTopic(String topic, int qos) {
		this.topicLock.lock();
		try {
			super.addTopic(topic, qos);
			if (this.client != null && this.client.isConnected()) {
				this.client.subscribe(topic, qos, this::messageArrived)
						.waitForCompletion(getCompletionTimeout());
			}
		}
		catch (MqttException e) {
			super.removeTopic(topic);
			throw new MessagingException("Failed to subscribe to topic " + topic, e);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public void removeTopic(String... topic) {
		this.topicLock.lock();
		try {
			if (this.client != null && this.client.isConnected()) {
				this.client.unsubscribe(topic).waitForCompletion(getCompletionTimeout());
			}
			super.removeTopic(topic);
		}
		catch (MqttException e) {
			throw new MessagingException("Failed to unsubscribe from topic(s) " + Arrays.toString(topic), e);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	private void subscribe() {
		this.topicLock.lock();
		String[] topics = getTopic();
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		try {
			if (topics.length > 0) {
				int[] requestedQos = getQos();
				IMqttMessageListener listener = this::messageArrived;
				IMqttMessageListener[] listeners = Stream.of(topics)
						.map(t -> listener)
						.toArray(IMqttMessageListener[]::new);
				IMqttToken subscribeToken = this.client.subscribe(topics, requestedQos, listeners);
				subscribeToken.waitForCompletion(getCompletionTimeout());
				int[] grantedQos = subscribeToken.getGrantedQos();
				if (grantedQos.length == 1 && grantedQos[0] == 0x80) { // NOSONAR
					throw new MqttException(MqttException.REASON_CODE_SUBSCRIBE_FAILED);
				}
				warnInvalidQosForSubscription(topics, requestedQos, grantedQos);
			}
		}
		catch (MqttException ex) {

			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
			}
			logger.error(ex, () -> "Error subscribing to " + Arrays.toString(topics));
		}
		finally {
			this.topicLock.unlock();
		}
		if (this.client.isConnected()) {
			String message = "Connected and subscribed to " + Arrays.toString(topics);
			logger.debug(message);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, message));
			}
		}
	}

	private void warnInvalidQosForSubscription(String[] topics, int[] requestedQos, int[] grantedQos) {
		for (int i = 0; i < requestedQos.length; i++) {
			if (grantedQos[i] != requestedQos[i]) {
				logger.warn(() -> "Granted QOS different to Requested QOS; topics: " + Arrays.toString(topics)
						+ " requested: " + Arrays.toString(requestedQos)
						+ " granted: " + Arrays.toString(grantedQos));
				break;
			}
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		this.lock.lock();
		try {
			if (isRunning()) {
				this.logger.error(() -> "Lost connection: " + cause.getMessage());
				ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
				}
			}
			else {
				// The 'connectComplete()' re-subscribes or sets this flag otherwise.
				this.readyToSubscribeOnStart = false;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) {
		AbstractIntegrationMessageBuilder<?> builder = toMessageBuilder(topic, mqttMessage);
		if (builder != null) {
			if (isManualAcks()) {
				builder.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
						new AcknowledgmentImpl(mqttMessage.getId(), mqttMessage.getQos(), this.client));
			}
			Message<?> message = builder.build();
			try {
				sendMessage(message);
			}
			catch (RuntimeException ex) {
				logger.error(ex, () -> "Unhandled exception for " + message);
				throw ex;
			}
		}
	}

	private AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage) {
		AbstractIntegrationMessageBuilder<?> builder = null;
		Exception conversionError = null;
		try {
			builder = getConverter().toMessageBuilder(topic, mqttMessage);
		}
		catch (Exception ex) {
			conversionError = ex;
		}

		if (builder == null && conversionError == null) {
			conversionError = new IllegalStateException("'MqttMessageConverter' returned 'null'");
		}

		if (conversionError != null) {
			GenericMessage<MqttMessage> message = new GenericMessage<>(mqttMessage);
			if (!sendErrorMessageIfNecessary(message, conversionError)) {
				MessageConversionException conversionException;
				if (conversionError instanceof MessageConversionException) {
					conversionException = (MessageConversionException) conversionError;
				}
				else {
					conversionException = new MessageConversionException(message, "Failed to convert from MQTT Message",
							conversionError);
				}
				throw conversionException;
			}
		}
		return builder;
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	@Override
	public void connectComplete(boolean isReconnect) {
		connectComplete(isReconnect, getUrl());
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		// The 'running' flag is set after 'doStart()', so possible a race condition
		// when start is not finished yet, but server answers with successful connection.
		if (isActive()) {
			subscribe();
		}
		else {
			this.readyToSubscribeOnStart = true;
		}
	}

	/**
	 * Used to complete message arrival when {@link #isManualAcks()} is true.
	 *
	 * @since 5.3
	 */
	private static class AcknowledgmentImpl implements SimpleAcknowledgment {

		private final int id;

		private final int qos;

		private final IMqttAsyncClient ackClient;

		/**
		 * Construct an instance with the provided properties.
		 * @param id the message id.
		 * @param qos the message QOS.
		 * @param client the client.
		 */
		AcknowledgmentImpl(int id, int qos, IMqttAsyncClient client) {
			this.id = id;
			this.qos = qos;
			this.ackClient = client;
		}

		@Override
		public void acknowledge() {
			if (this.ackClient != null) {
				try {
					this.ackClient.messageArrivedComplete(this.id, this.qos);
				}
				catch (MqttException e) {
					throw new IllegalStateException(e);
				}
			}
			else {
				throw new IllegalStateException("Client has changed");
			}
		}

	}

}
