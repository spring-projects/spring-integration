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

package org.springframework.integration.zeromq.inbound;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.zeromq.core.ConsumerStopAction;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Jeromq Implementation.
 *
 * @author Subhobrata Dey
 *
 * @since 5.1
 *
 */
public class ZeromqMessageDrivenChannelAdapter extends AbstractZeromqMessageDrivenChannelAdapter
				implements ApplicationEventPublisherAware, Runnable {

	private static final int DEFAULT_RECOVERY_INTERVAL = 10000;

	private final org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory;

	private volatile ZMQ.Context context;

	private volatile ZMQ.Socket client;

	private volatile ZMQ.Poller poller;

	private volatile ScheduledFuture<?> reconnectFuture;

	private volatile boolean connected;

	private volatile int recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private volatile boolean cleanSession;

	private volatile ConsumerStopAction consumerStopAction;

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Use this constructor
	 * if the client Factory is provided by the {@link org.springframework.integration.zeromq.core.DefaultZeromqClientFactory}.
	 * @param clientFactory The client factory.
	 */
	public ZeromqMessageDrivenChannelAdapter(org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory) {
		super(clientFactory.getServerURI(), clientFactory.getClientId());
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor for a single url (although it may be overridden
	 * if the server URI is provided by the {@link org.springframework.integration.zeromq.core.DefaultZeromqClientFactory}.
	 * @param url the URL.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 */
	public ZeromqMessageDrivenChannelAdapter(String url, String clientId, org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory) {
		super(url, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this constructor if the server URI is provided by the {@link org.springframework.integration.zeromq.core.ZeromqClientFactory}.
	 * @param clientId The client id.
	 * @param clientFactory The client factory.
	 * @since 5.0.1
	 */
	public ZeromqMessageDrivenChannelAdapter(String clientId, org.springframework.integration.zeromq.core.ZeromqClientFactory clientFactory) {
		super(null, clientId);
		this.clientFactory = clientFactory;
	}

	/**
	 * Use this URL when you don't need additional {@link org.springframework.integration.zeromq.core.ZeromqClientFactory}.
	 * @param url The URL.
	 * @param clientId The client id.
	 */
	public ZeromqMessageDrivenChannelAdapter(String url, String clientId) {
		this(url, clientId, new org.springframework.integration.zeromq.core.DefaultZeromqClientFactory(SocketType.SUB.type()));
	}

	/**
	 * The time (ms) to wait between reconnection attempts.
	 * Default {@value #DEFAULT_RECOVERY_INTERVAL}.
	 * @param recoveryInterval the interval.
	 * @since 5.0.1
	 */
	public void setRecoveryInterval(int recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	/**
	 * @since 5.0.1
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void doStart() {
		Assert.state(getTaskScheduler() != null, "A 'taskScheduler' is required");
		super.doStart();
		try {
			connectAndSubscribe();
		}
		catch (Exception e) {
			connectionLost(e);
		}
	}

	public synchronized void connectionLost(Throwable cause) {
		logger.error("Exception while connecting and subscribing, retrying", cause);
		this.connected = false;
		this.scheduleReconnect();
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new org.springframework.integration.zeromq.event.ZeromqConnectionFailedEvent(this, cause));
		}
	}

	@Override
	protected void doStop() {
		cancelReconnect();
		super.doStop();
		if (this.client != null) {
			try {
				if (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_ALWAYS) ||
						this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_CLEAN) &&
						this.cleanSession) {
					this.client.unsubscribe(getTopic().getBytes(Charset.defaultCharset()));
				}
			}
			catch (ZMQException e) {
				logger.error("Exception while unsubscribing", e);
			}
			try {
				if (getUrl() != null) {
					this.client.disconnect(getUrl());
				}
				else {
					this.client.disconnect(this.clientFactory.getServerURI());
				}
			}
			catch (ZMQException e) {
				logger.error("Exception while disconnecting", e);
			}
			try {
				this.poller.close();
				this.client.close();
				this.context.term();
			}
			catch (ZMQException e) {
				logger.error("Exception while closing", e);
			}
			this.connected = false;
			this.client = null;
		}
	}

	@Override
	public void setTopic(String topic) {
		this.topicLock.lock();
		try {
			super.setTopic(topic);
			if (this.client != null) {
				this.client.subscribe(topic.getBytes(Charset.defaultCharset()));
			}
		}
		catch (ZMQException e) {
			super.removeTopic(topic);
			throw new MessagingException("Failed to subscribe to topic " + topic, e);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	@Override
	public void removeTopic(String topic) {
		this.topicLock.lock();
		try {
			if (this.client != null) {
				this.client.unsubscribe(topic.getBytes(Charset.defaultCharset()));
				if (this.clientFactory.getClientType() == SocketType.SUB.type()) {
					this.client.subscribe("".getBytes(Charset.defaultCharset()));
				}
			}
			super.removeTopic(topic);
		}
		catch (ZMQException e) {
			throw new MessagingException("Failed to unsubscribe from topic " + topic, e);
		}
		finally {
			this.topicLock.unlock();
		}
	}

	private synchronized void connectAndSubscribe() {
		this.cleanSession = this.clientFactory.cleanSession();
		this.consumerStopAction = this.clientFactory.getConsumerStopAction();
		if (this.consumerStopAction == null) {
			this.consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;
		}
		Assert.state(getUrl() != null || this.clientFactory.getServerURI() != null,
				"If no 'url' provided, clientFactory.getServerURI() must not be null");
		this.context = this.clientFactory.getContext();
		this.client = this.clientFactory.getClientInstance(getClientId(), getTopic());

		if (this.clientFactory.getUserName() != null && this.clientFactory.getPassword() != null) {
			this.client.setPlainUsername(this.clientFactory.getUserName().getBytes(Charset.defaultCharset()));
			this.client.setPlainPassword(this.clientFactory.getPassword().getBytes(Charset.defaultCharset()));
		}

		this.topicLock.lock();
		String topic = getTopic();

		try {
			if (getUrl() != null) {
				this.client.connect(getUrl());
			}
			else {
				this.client.connect(this.clientFactory.getServerURI());
			}

			this.poller = this.clientFactory.getPollerInstance(ZMQ.Poller.POLLIN);
		}
		catch (ZMQException e) {
			if (this.applicationEventPublisher != null) {
				this.applicationEventPublisher.publishEvent(new org.springframework.integration.zeromq.event.ZeromqConnectionFailedEvent(this, e));
			}
			logger.error("Error connecting or subscribing to " + topic, e);
			this.client.close();
			throw new ZMQException(e.getMessage(), e.hashCode());
		}
		finally {
			this.topicLock.unlock();
		}
		this.connected = true;
		String message = "Connected and subscribed to " + topic;
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new org.springframework.integration.zeromq.event.ZeromqSubscribedEvent(this, message));
		}
		ExecutorService executorService = Executors.newFixedThreadPool(this.clientFactory.getIoThreads());
		executorService.submit(this);
	}

	@Override
	public void run() {
		pollForMessages();
	}

	private void pollForMessages() {
		while (!Thread.currentThread().isInterrupted()) {
			this.poller.poll();
			if (this.poller.pollin(0)) {
				byte[] receivedMessage = this.client.recv(0);
				Message<?> message = this.getConverter().toMessage(getTopic(), receivedMessage);
				try {
					sendMessage(message);
				}
				catch (RuntimeException e) {
					logger.error("Unhandled exception for " + message.toString(), e);
					throw e;
				}
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
			this.reconnectFuture = getTaskScheduler().schedule(() -> {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Attempting reconnect");
					}
					synchronized (org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter.this) {
						if (!org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter.this.connected) {
							connectAndSubscribe();
							org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter.this.reconnectFuture = null;
						}
					}
				}
				catch (ZMQException e) {
					logger.error("Exception while connecting and subscribing", e);
					scheduleReconnect();
				}
			}, new Date(System.currentTimeMillis() + this.recoveryInterval));
		}
		catch (Exception e) {
			logger.error("Failed to schedule reconnect", e);
		}
	}
}
