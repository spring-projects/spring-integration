/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Eclipse Paho Implementation.
 *
 * @author Gary Russell
 * @since 1.0
 *
 */
public class MqttPahoMessageDrivenChannelAdapter extends AbstractMqttMessageDrivenChannelAdapter
		implements MqttCallback, ApplicationEventPublisherAware {

	private static final int DEFAULT_COMPLETION_TIMEOUT = 30000;

	private static final int DEFAULT_RECOVERY_INTERVAL = 10000;

	private final MqttPahoClientFactory clientFactory;

	private volatile IMqttAsyncClient client;

	private volatile ScheduledFuture<?> reconnectFuture;

	private volatile boolean connected;

	private volatile int completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

	private volatile int recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private volatile boolean cleanSession;

	private volatile ConsumerStopAction consumerStopAction;

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Use this constructor for a single url (although it may be overridden
	 * if the server URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()}
	 * provided by the {@link MqttPahoClientFactory}).
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
	 * Use this constructor if the server URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()}
	 * provided by the {@link MqttPahoClientFactory}.
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
	 * Use this URL when you don't need additional {@link MqttConnectOptions}.
	 * @param url The URL.
	 * @param clientId The client id.
	 * @param topic The topic(s).
	 */
	public MqttPahoMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		this(url, clientId, new DefaultMqttPahoClientFactory(), topic);
	}

	/**
	 * Set the completion timeout for async operations. Not settable using the namespace.
	 * Default 30000 milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 4.1
	 */
	public void setCompletionTimeout(int completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	/**
	 * The time (ms) to wait between reconnection attempts.
	 * Default {@value #DEFAULT_RECOVERY_INTERVAL}.
	 * @param recoveryInterval the interval.
	 * @since 4.2.2
	 */
	public void setRecoveryInterval(int recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	/**
	 * @since 4.2.2
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void doStart() {
		super.doStart();
		try {
			connectAndSubscribe();
		}
		catch (Exception e) {
			logger.error("Exception while connecting and subscribing, retrying", e);
			this.scheduleReconnect();
		}
	}

	@Override
	protected void doStop() {
		cancelReconnect();
		super.doStop();
		if (this.client != null) {
			try {
				if (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_ALWAYS)
						|| (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_CLEAN)
								&& this.cleanSession)) {
					this.client.unsubscribe(getTopic())
							.waitForCompletion(this.completionTimeout);
				}
			}
			catch (MqttException e) {
				logger.error("Exception while unsubscribing", e);
			}
			try {
				this.client.disconnect()
						.waitForCompletion(this.completionTimeout);
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
	}

	@Override
	public void addTopic(String topic, int qos) {
		this.topicLock.lock();
		try {
			super.addTopic(topic, qos);
			if (this.client != null && this.client.isConnected()) {
				this.client.subscribe(topic, qos)
						.waitForCompletion(this.completionTimeout);
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
				this.client.unsubscribe(topic)
						.waitForCompletion(this.completionTimeout);
			}
			super.removeTopic(topic);
		}
		catch (MqttException e) {
			throw new MessagingException("Failed to unsubscribe from topic " + Arrays.asList(topic), e);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	private void connectAndSubscribe() throws MqttException {
		MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
		this.cleanSession = connectionOptions.isCleanSession();
		this.consumerStopAction = this.clientFactory.getConsumerStopAction();
		if (this.consumerStopAction == null) {
			this.consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;
		}
		Assert.state(getUrl() != null || connectionOptions.getServerURIs() != null,
				"If no 'url' provided, connectionOptions.getServerURIs() must not be null");
		this.client = this.clientFactory.getAsyncClientInstance(getUrl(), getClientId());
		this.client.setCallback(this);

		this.topicLock.lock();
		try {
			this.client.connect(connectionOptions)
					.waitForCompletion(this.completionTimeout);
			this.client.subscribe(getTopic(), getQos())
					.waitForCompletion(this.completionTimeout);
		}
		catch (MqttException e) {
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, e));
			}
			logger.error("Error connecting or subscribing to " + Arrays.asList(getTopic()), e);
			this.client.disconnect()
					.waitForCompletion(this.completionTimeout);
			throw e;
		}
		finally {
			this.topicLock.unlock();
		}
		if (this.client.isConnected()) {
			this.connected = true;
			String message = "Connected and subscribed to " + Arrays.asList(getTopic());
			if (logger.isDebugEnabled()) {
				logger.debug(message);
			}
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, message));
			}
			// cancel() after the publish in case we are on that thread; a send to a QueueChannel would fail.
			if (this.reconnectFuture != null) {
				cancelReconnect();
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
						if (!MqttPahoMessageDrivenChannelAdapter.this.connected) {
							connectAndSubscribe();
						}
					}
					catch (MqttException e) {
						logger.error("Exception while connecting and subscribing", e);
					}
				}

			}, this.recoveryInterval);
		}
		catch (Exception e) {
			logger.error("Failed to schedule reconnect", e);
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		this.logger.error("Lost connection:" + cause.getMessage() + "; retrying...");
		this.connected = false;
		scheduleReconnect();
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
		Message<?> message = this.getConverter().toMessage(topic, mqttMessage);
		try {
			sendMessage(message);
		}
		catch (RuntimeException e) {
			logger.error("Unhandled exception for " + message.toString(), e);
			throw e;
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

}
