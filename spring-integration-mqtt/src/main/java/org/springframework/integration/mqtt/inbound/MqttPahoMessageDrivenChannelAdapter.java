/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
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
 *
 * @since 4.0
 *
 */
public class MqttPahoMessageDrivenChannelAdapter extends AbstractMqttMessageDrivenChannelAdapter
		implements MqttCallback, MqttPahoComponent, ApplicationEventPublisherAware {

	/**
	 * The default completion timeout in milliseconds.
	 */
	public static final long DEFAULT_COMPLETION_TIMEOUT = 30_000L;

	/**
	 * The default disconnect completion timeout in milliseconds.
	 */
	public static final long DISCONNECT_COMPLETION_TIMEOUT = 5_000L;

	private static final int DEFAULT_RECOVERY_INTERVAL = 10_000;

	private final MqttPahoClientFactory clientFactory;

	private int recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private long completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

	private long disconnectCompletionTimeout = DISCONNECT_COMPLETION_TIMEOUT;

	private boolean manualAcks;

	private volatile IMqttClient client;

	private volatile ScheduledFuture<?> reconnectFuture;

	private volatile boolean connected;

	private volatile boolean cleanSession;

	private volatile ConsumerStopAction consumerStopAction;

	private ApplicationEventPublisher applicationEventPublisher;

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
	 * Use this constructor when you don't need additional {@link MqttConnectOptions}.
	 * @param url The URL.
	 * @param clientId The client id.
	 * @param topic The topic(s).
	 */
	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		this(url, clientId, new DefaultMqttPahoClientFactory(), topic);
	}

	/**
	 * Set the completion timeout for operations. Not settable using the namespace.
	 * Default {@value #DEFAULT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 4.1
	 */
	public synchronized void setCompletionTimeout(long completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	/**
	 * Set the completion timeout when disconnecting. Not settable using the namespace.
	 * Default {@value #DISCONNECT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 5.1.10
	 */
	public synchronized void setDisconnectCompletionTimeout(long completionTimeout) {
		this.disconnectCompletionTimeout = completionTimeout;
	}

	/**
	 * The time (ms) to wait between reconnection attempts.
	 * Default {@value #DEFAULT_RECOVERY_INTERVAL}.
	 * @param recoveryInterval the interval.
	 * @since 4.2.2
	 */
	public synchronized void setRecoveryInterval(int recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	/**
	 * Set the acknowledgment mode to manual.
	 * @param manualAcks true for manual acks.
	 * @since 5.3
	 */
	public void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	/**
	 * @since 4.2.2
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher; // NOSONAR (inconsistent synchronization)
	}

	@Override
	public MqttConnectOptions getConnectionInfo() {
		MqttConnectOptions options = this.clientFactory.getConnectionOptions();
		if (options.getServerURIs() == null) {
			String url = getUrl();
			if (url != null) {
				options = MqttUtils.cloneConnectOptions(options);
				options.setServerURIs(new String[]{ url });
			}
		}
		return options;
	}

	@Override
	protected void doStart() {
		Assert.state(getTaskScheduler() != null, "A 'taskScheduler' is required");
		try {
			connectAndSubscribe();
		}
		catch (Exception ex) {
			logger.error(ex, "Exception while connecting and subscribing, retrying");
			scheduleReconnect();
		}
	}

	@Override
	protected synchronized void doStop() {
		cancelReconnect();
		if (this.client != null) {
			try {
				if (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_ALWAYS)
						|| (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_CLEAN)
						&& this.cleanSession)) {

					this.client.unsubscribe(getTopic());
				}
			}
			catch (MqttException ex) {
				logger.error(ex, "Exception while unsubscribing");
			}
			try {
				this.client.disconnectForcibly(this.disconnectCompletionTimeout);
			}
			catch (MqttException ex) {
				logger.error(ex, "Exception while disconnecting");
			}

			this.client.setCallback(null);

			try {
				this.client.close();
			}
			catch (MqttException ex) {
				logger.error(ex, "Exception while closing");
			}
			this.connected = false;
			this.client = null;
		}
	}

	@Override
	public void addTopic(String topic, int qos) {
		this.topicLock.lock();
		try {
			super.addTopic(topic, qos);
			if (this.client != null && this.client.isConnected()) {
				this.client.subscribe(topic, qos);
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
				this.client.unsubscribe(topic);
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

	private synchronized void connectAndSubscribe() throws MqttException {
		MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
		this.cleanSession = connectionOptions.isCleanSession();
		this.consumerStopAction = this.clientFactory.getConsumerStopAction();
		if (this.consumerStopAction == null) {
			this.consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;
		}
		Assert.state(getUrl() != null || connectionOptions.getServerURIs() != null,
				"If no 'url' provided, connectionOptions.getServerURIs() must not be null");
		this.client = this.clientFactory.getClientInstance(getUrl(), getClientId());
		this.client.setCallback(this);
		if (this.client instanceof MqttClient) {
			((MqttClient) this.client).setTimeToWait(this.completionTimeout);
		}

		this.topicLock.lock();
		String[] topics = getTopic();
		try {
			this.client.connect(connectionOptions);
			this.client.setManualAcks(this.manualAcks);
			int[] requestedQos = getQos();
			int[] grantedQos = Arrays.copyOf(requestedQos, requestedQos.length);
			this.client.subscribe(topics, grantedQos);
			warnInvalidQosForSubscription(topics, requestedQos, grantedQos);
		}
		catch (MqttException ex) {
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
			}
			logger.error(ex, () -> "Error connecting or subscribing to " + Arrays.toString(topics));
			this.client.disconnectForcibly(this.disconnectCompletionTimeout);
			try {
				this.client.setCallback(null);
				this.client.close();
			}
			catch (MqttException e1) {
				// NOSONAR
			}
			this.client = null;
			throw ex;
		}
		finally {
			this.topicLock.unlock();
		}
		if (this.client.isConnected()) {
			this.connected = true;
			String message = "Connected and subscribed to " + Arrays.toString(topics);
			logger.debug(message);
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, message));
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

	private synchronized void cancelReconnect() {
		if (this.reconnectFuture != null) {
			this.reconnectFuture.cancel(false);
			this.reconnectFuture = null;
		}
	}

	private synchronized void scheduleReconnect() {
		cancelReconnect();
		try {
			this.reconnectFuture = getTaskScheduler().schedule(() -> {
				try {
					logger.debug("Attempting reconnect");
					synchronized (MqttPahoMessageDrivenChannelAdapter.this) {
						if (!MqttPahoMessageDrivenChannelAdapter.this.connected) {
							connectAndSubscribe();
							MqttPahoMessageDrivenChannelAdapter.this.reconnectFuture = null;
						}
					}
				}
				catch (MqttException ex) {
					logger.error(ex, "Exception while connecting and subscribing");
					scheduleReconnect();
				}
			}, new Date(System.currentTimeMillis() + this.recoveryInterval));
		}
		catch (Exception ex) {
			logger.error(ex, "Failed to schedule reconnect");
		}
	}

	@Override
	public synchronized void connectionLost(Throwable cause) {
		if (isRunning()) {
			this.logger.error(() -> "Lost connection: " + cause.getMessage() + "; retrying...");
			this.connected = false;
			if (this.client != null) {
				try {
					this.client.setCallback(null);
					this.client.close();
				}
				catch (MqttException e) {
					// NOSONAR
				}
			}
			this.client = null;
			scheduleReconnect();
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
			}
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) {
		AbstractIntegrationMessageBuilder<?> builder = toMessageBuilder(topic, mqttMessage);
		if (builder != null) {
			if (this.manualAcks) {
				builder.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
						new AcknowledgmentImpl(mqttMessage.getId(), mqttMessage.getQos(), this.client));
			}
			Message<?> message = builder.build();
			try {
				sendMessage(message);
			}
			catch (RuntimeException ex) {
				logger.error(ex, () -> "Unhandled exception for " + message.toString());
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

	/**
	 * Used to complete message arrival when {@link #manualAcks} is true.
	 *
	 * @since 5.3
	 */
	private static class AcknowledgmentImpl implements SimpleAcknowledgment {

		private final int id;

		private final int qos;

		private final IMqttClient ackClient;

		/**
		 * Construct an instance with the provided properties.
		 * @param id the message id.
		 * @param qos the message QOS.
		 * @param client the client.
		 */
		AcknowledgmentImpl(int id, int qos, IMqttClient client) {
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
