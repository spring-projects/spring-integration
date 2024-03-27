/*
 * Copyright 2021-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.mqtt.core.ClientManager;
import org.springframework.integration.mqtt.core.MqttComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttProtocolErrorEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * The {@link AbstractMqttMessageDrivenChannelAdapter} implementation for MQTT v5.
 * <p>
 * The {@link MqttProperties} are mapped via the provided {@link HeaderMapper};
 * meanwhile the regular {@link MqttMessage} properties are always mapped into headers.
 * <p>
 * It is recommended to have the {@link MqttConnectionOptions#setAutomaticReconnect(boolean)}
 * set to true to let an internal {@link IMqttAsyncClient} instance to handle reconnects.
 * Otherwise, only the manual restart of this component can handle reconnects, e.g. via
 * {@link MqttConnectionFailedEvent} handling on disconnection.
 * <p>
 * See {@link #setPayloadType} for more information about type conversion.
 *
 * @author Artem Bilan
 * @author Mikhail Polivakha
 * @author Lucas Bowler
 * @author Artem Vozhdayenko
 * @author Matthias Thoma
 *
 * @since 5.5.5
 *
 */
public class Mqttv5PahoMessageDrivenChannelAdapter
		extends AbstractMqttMessageDrivenChannelAdapter<IMqttAsyncClient, MqttConnectionOptions>
		implements MqttCallback, MqttComponent<MqttConnectionOptions> {

	private final Lock lock = new ReentrantLock();

	private final MqttConnectionOptions connectionOptions;

	private List<MqttSubscription> subscriptions;

	private IMqttAsyncClient mqttClient;

	@Nullable
	private MqttClientPersistence persistence;

	private SmartMessageConverter messageConverter;

	private Class<?> payloadType = byte[].class;

	private HeaderMapper<MqttProperties> headerMapper = new MqttHeaderMapper();

	private volatile boolean readyToSubscribeOnStart;

	private final AtomicInteger subscriptionIdentifierCounter = new AtomicInteger(0);

	/**
	 * Create an instance based on the MQTT url, client id and subscriptions.
	 * @param url the MQTT url to connect.
	 * @param clientId the unique client id.
	 * @param mqttSubscriptions the MQTT subscriptions.
	 * @since 6.3
	 */
	public Mqttv5PahoMessageDrivenChannelAdapter(String url, String clientId, MqttSubscription... mqttSubscriptions) {
		this(url, clientId, Arrays.stream(mqttSubscriptions).map(MqttSubscription::getTopic).toArray(String[]::new));
		this.subscriptions = new ArrayList<>();
		Collections.addAll(this.subscriptions, mqttSubscriptions);
	}

	public Mqttv5PahoMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		super(url, clientId, topic);
		Assert.hasText(url, "'url' cannot be null or empty");
		this.connectionOptions = new MqttConnectionOptions();
		this.connectionOptions.setServerURIs(new String[] {url});
		this.connectionOptions.setAutomaticReconnect(true);
	}

	/**
	 * Create an instance based on the MQTT connection options, client id and subscriptions.
	 * @param connectionOptions the MQTT connection options.
	 * @param clientId the unique client id.
	 * @param mqttSubscriptions the MQTT subscriptions.
	 * @since 6.3
	 */
	public Mqttv5PahoMessageDrivenChannelAdapter(MqttConnectionOptions connectionOptions, String clientId,
			MqttSubscription... mqttSubscriptions) {

		this(connectionOptions, clientId,
				Arrays.stream(mqttSubscriptions).map(MqttSubscription::getTopic).toArray(String[]::new));
		this.subscriptions = new ArrayList<>();
		Collections.addAll(this.subscriptions, mqttSubscriptions);
	}

	public Mqttv5PahoMessageDrivenChannelAdapter(MqttConnectionOptions connectionOptions, String clientId,
			String... topic) {

		super(obtainServerUrlFromOptions(connectionOptions), clientId, topic);
		this.connectionOptions = connectionOptions;
		if (!this.connectionOptions.isAutomaticReconnect()) {
			logger.warn("It is recommended to set 'automaticReconnect' MQTT client option. " +
					"Otherwise the current channel adapter restart should be used explicitly, " +
					"e.g. via handling 'MqttConnectionFailedEvent' on client disconnection.");
		}
	}

	/**
	 * Create an instance based on the client manager and subscriptions.
	 * @param clientManager The client manager.
	 * @param mqttSubscriptions the MQTT subscriptions.
	 * @since 6.3
	 */
	public Mqttv5PahoMessageDrivenChannelAdapter(ClientManager<IMqttAsyncClient, MqttConnectionOptions> clientManager,
			MqttSubscription... mqttSubscriptions) {

		this(clientManager, Arrays.stream(mqttSubscriptions).map(MqttSubscription::getTopic).toArray(String[]::new));
		this.subscriptions = new ArrayList<>();
		Collections.addAll(this.subscriptions, mqttSubscriptions);
	}

	/**
	 * Use this constructor when you need to use a single {@link ClientManager}
	 * (for instance, to reuse an MQTT connection).
	 * @param clientManager The client manager.
	 * @param topic The topic(s).
	 * @since 6.0
	 */
	public Mqttv5PahoMessageDrivenChannelAdapter(ClientManager<IMqttAsyncClient, MqttConnectionOptions> clientManager,
			String... topic) {

		super(clientManager, topic);
		this.connectionOptions = clientManager.getConnectionInfo();
	}

	@Override
	public MqttConnectionOptions getConnectionInfo() {
		return this.connectionOptions;
	}

	public void setPersistence(@Nullable MqttClientPersistence persistence) {
		this.persistence = persistence;
	}

	@Override
	public void setConverter(MqttMessageConverter converter) {
		throw new UnsupportedOperationException("Use setMessageConverter(SmartMessageConverter) instead");
	}

	public void setMessageConverter(SmartMessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the type of the target message payload to produce after conversion from MQTT message.
	 * Defaults to {@code byte[].class} - just extract MQTT message payload without conversion.
	 * Can be set to {@link MqttMessage} class to produce the whole MQTT message as a payload.
	 * @param payloadType the expected payload type to convert MQTT message to.
	 */
	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null.");
		this.payloadType = payloadType;
	}

	public void setHeaderMapper(HeaderMapper<MqttProperties> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (getClientManager() == null && this.mqttClient == null) {
			try {
				this.mqttClient = new MqttAsyncClient(getUrl(), getClientId(), this.persistence);
				this.mqttClient.setCallback(this);
				this.mqttClient.setManualAcks(isManualAcks());
			}
			catch (MqttException ex) {
				throw new BeanCreationException("Cannot create 'MqttAsyncClient' for: " + getComponentName(), ex);
			}
		}
		if (this.messageConverter == null) {
			setMessageConverter(getBeanFactory()
					.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
							SmartMessageConverter.class));
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
		catch (MqttException ex) {
			if (getConnectionInfo().isAutomaticReconnect()) {
				try {
					this.mqttClient.reconnect();
				}
				catch (MqttException re) {
					logger.error(re, "MQTT client failed to connect. Never happens.");
				}
			}
			else {
				ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
				}
				logger.error(ex, "MQTT client failed to connect.");
			}
		}
	}

	private void connect() throws MqttException {
		this.lock.lock();
		try {
			var clientManager = getClientManager();
			if (clientManager == null) {
				this.mqttClient.connect(this.connectionOptions).waitForCompletion(getCompletionTimeout());
			}
			else {
				this.mqttClient = clientManager.getClient();
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	protected void doStop() {
		this.topicLock.lock();
		this.readyToSubscribeOnStart = false;
		String[] topics = getTopic();
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				if (this.connectionOptions.isCleanStart()) {
					this.mqttClient.unsubscribe(topics).waitForCompletion(getCompletionTimeout());
					// Have to re-subscribe on next start if connection is not lost.
					this.readyToSubscribeOnStart = true;

				}
				if (getClientManager() == null) {
					this.mqttClient.disconnectForcibly(getDisconnectCompletionTimeout());
				}
			}
		}
		catch (MqttException ex) {
			logger.error(ex, () -> "Error unsubscribing from " + Arrays.toString(topics));
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			if (getClientManager() == null && this.mqttClient != null) {
				this.mqttClient.close(true);
			}
		}
		catch (MqttException ex) {
			logger.error(ex, "Failed to close 'MqttAsyncClient'");
		}
	}

	@Override
	public void setQos(int... qos) {
		Assert.isNull(this.subscriptions, "The 'qos' must be provided with the 'MqttSubscription'.");
		super.setQos(qos);
	}

	@Override
	public void addTopic(String topic, int qos) {
		this.topicLock.lock();
		try {
			super.addTopic(topic, qos);
			MqttSubscription subscription = new MqttSubscription(topic, qos);
			if (this.subscriptions != null) {
				this.subscriptions.add(subscription);
			}
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				MqttProperties subscriptionProperties = new MqttProperties();
				subscriptionProperties.setSubscriptionIdentifier(this.subscriptionIdentifierCounter.incrementAndGet());
				this.mqttClient.subscribe(new MqttSubscription[] {subscription},
								null, null, new IMqttMessageListener[] {this::messageArrived}, subscriptionProperties)
						.waitForCompletion(getCompletionTimeout());
			}
		}
		catch (MqttException ex) {
			throw new MessagingException("Failed to subscribe to topic " + topic, ex);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public void removeTopic(String... topic) {
		this.topicLock.lock();
		try {
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				this.mqttClient.unsubscribe(topic).waitForCompletion(getCompletionTimeout());
			}
			super.removeTopic(topic);
			if (!CollectionUtils.isEmpty(this.subscriptions)) {
				this.subscriptions.removeIf((sub) -> ObjectUtils.containsElement(topic, sub.getTopic()));
			}
		}
		catch (MqttException ex) {
			throw new MessagingException("Failed to unsubscribe from topic(s) " + Arrays.toString(topic), ex);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) {
		Map<String, Object> headers = this.headerMapper.toHeaders(mqttMessage.getProperties());
		headers.put(MqttHeaders.ID, mqttMessage.getId());
		headers.put(MqttHeaders.RECEIVED_QOS, mqttMessage.getQos());
		headers.put(MqttHeaders.DUPLICATE, mqttMessage.isDuplicate());
		headers.put(MqttHeaders.RECEIVED_RETAINED, mqttMessage.isRetained());
		headers.put(MqttHeaders.RECEIVED_TOPIC, topic);

		if (isManualAcks()) {
			headers.put(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
					new AcknowledgmentImpl(mqttMessage.getId(), mqttMessage.getQos(), this.mqttClient));
		}

		Object payload =
				MqttMessage.class.isAssignableFrom(this.payloadType)
						? mqttMessage
						: mqttMessage.getPayload();

		Message<?> message;
		if (MqttMessage.class.isAssignableFrom(this.payloadType) || byte[].class.isAssignableFrom(this.payloadType)) {
			message = new GenericMessage<>(payload, headers);
		}
		else {
			message = this.messageConverter.toMessage(payload, new MessageHeaders(headers), this.payloadType);
		}

		try {
			sendMessage(message);
		}
		catch (RuntimeException ex) {
			logger.error(ex, () -> "Unhandled exception for " + message);
			throw ex;
		}
	}

	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		if (isRunning()) {
			MqttException cause = disconnectResponse.getException();
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

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		if (applicationEventPublisher != null) {
			applicationEventPublisher.publishEvent(new MqttProtocolErrorEvent(this, exception));
		}
	}

	@Override
	public void deliveryComplete(IMqttToken token) {

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

	private void subscribe() {
		var clientManager = getClientManager();
		if (clientManager != null && this.mqttClient == null) {
			this.mqttClient = clientManager.getClient();
		}

		MqttSubscription[] mqttSubscriptions = obtainSubscriptions();
		if (ObjectUtils.isEmpty(mqttSubscriptions)) {
			return;
		}
		ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
		this.topicLock.lock();
		try {
			IMqttMessageListener listener = this::messageArrived;
			IMqttMessageListener[] listeners = IntStream.range(0, mqttSubscriptions.length)
					.mapToObj(t -> listener)
					.toArray(IMqttMessageListener[]::new);
			MqttProperties subscriptionProperties = new MqttProperties();
			subscriptionProperties.setSubscriptionIdentifiers(IntStream.range(0, mqttSubscriptions.length)
					.mapToObj(i -> this.subscriptionIdentifierCounter.incrementAndGet())
					.toList());
			this.mqttClient.subscribe(mqttSubscriptions, null, null, listeners, subscriptionProperties)
					.waitForCompletion(getCompletionTimeout());
			String message = "Connected and subscribed to " + Arrays.toString(mqttSubscriptions);
			logger.debug(message);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, message));
			}
		}
		catch (MqttException ex) {
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
			}
			logger.error(ex, () -> "Error subscribing to " + Arrays.toString(mqttSubscriptions));
		}
		finally {
			this.topicLock.unlock();
		}
	}

	private MqttSubscription[] obtainSubscriptions() {
		if (this.subscriptions != null) {
			return this.subscriptions.toArray(new MqttSubscription[0]);
		}
		else {
			String[] topics = getTopic();
			if (topics.length == 0) {
				return null;
			}
			int[] requestedQos = getQos();
			return IntStream.range(0, topics.length)
					.mapToObj(i -> new MqttSubscription(topics[i], requestedQos[i]))
					.toArray(MqttSubscription[]::new);
		}
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {

	}

	private static String obtainServerUrlFromOptions(MqttConnectionOptions connectionOptions) {
		Assert.notNull(connectionOptions, "'connectionOptions' must not be null");
		String[] serverURIs = connectionOptions.getServerURIs();
		Assert.notEmpty(serverURIs, "'serverURIs' must be provided in the 'MqttConnectionOptions'");
		return serverURIs[0];
	}

	/**
	 * Used to complete message arrival when {@link #isManualAcks()} is true.
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
			try {
				this.ackClient.messageArrivedComplete(this.id, this.qos);
			}
			catch (MqttException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
