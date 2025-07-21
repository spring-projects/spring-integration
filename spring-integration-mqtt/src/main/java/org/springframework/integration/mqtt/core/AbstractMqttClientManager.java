/*
 * Copyright 2022-present the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.mqtt.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT client managers which can be a base for any common v3/v5 client manager implementation.
 * Contains some basic utility and implementation-agnostic fields and methods.
 *
 * @param <T> MQTT client type
 * @param <C> MQTT connection options type (v5 or v3)
 *
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 6.0
 */
public abstract class AbstractMqttClientManager<T, C> implements ClientManager<T, C>, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR

	private static final int DEFAULT_MANAGER_PHASE = 0;

	protected final Lock lock = new ReentrantLock();

	private final Set<ConnectCallback> connectCallbacks = Collections.synchronizedSet(new HashSet<>());

	private final String clientId;

	private int phase = DEFAULT_MANAGER_PHASE;

	private long completionTimeout = ClientManager.DEFAULT_COMPLETION_TIMEOUT;

	private long disconnectCompletionTimeout = ClientManager.DISCONNECT_COMPLETION_TIMEOUT;

	private long quiescentTimeout = ClientManager.QUIESCENT_TIMEOUT;

	private boolean manualAcks;

	private ApplicationEventPublisher applicationEventPublisher;

	private String url;

	private String beanName;

	private T client;

	protected AbstractMqttClientManager(String clientId) {
		Assert.notNull(clientId, "'clientId' is required");
		this.clientId = clientId;
	}

	public void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	protected String getUrl() {
		return this.url;
	}

	protected void setUrl(String url) {
		this.url = url;
	}

	protected String getClientId() {
		return this.clientId;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	protected void setClient(T client) {
		this.lock.lock();
		try {
			this.client = client;
		}
		finally {
			this.lock.unlock();
		}
	}

	protected Set<ConnectCallback> getCallbacks() {
		return this.connectCallbacks;
	}

	/**
	 * Set the completion timeout for operations.
	 * Default {@value #DEFAULT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 6.0.3
	 */
	public void setCompletionTimeout(long completionTimeout) {
		this.completionTimeout = completionTimeout;
	}

	protected long getCompletionTimeout() {
		return this.completionTimeout;
	}

	/**
	 * Set the completion timeout when disconnecting.
	 * Default {@value #DISCONNECT_COMPLETION_TIMEOUT} milliseconds.
	 * @param completionTimeout The timeout.
	 * @since 6.0.3
	 */
	public void setDisconnectCompletionTimeout(long completionTimeout) {
		this.disconnectCompletionTimeout = completionTimeout;
	}

	protected long getDisconnectCompletionTimeout() {
		return this.disconnectCompletionTimeout;
	}

	/**
	 * Set the quiescentTimeout timeout when disconnecting.
	 * Default is {@link ClientManager#QUIESCENT_TIMEOUT} milliseconds.
	 * @param quiescentTimeout The timeout.
	 * @since 7.0.0
	 */
	public void setQuiescentTimeout(long quiescentTimeout) {
		this.quiescentTimeout = quiescentTimeout;
	}

	protected long getQuiescentTimeout() {
		return this.quiescentTimeout;
	}

	@Override
	public boolean isManualAcks() {
		return this.manualAcks;
	}

	@Override
	public T getClient() {
		this.lock.lock();
		try {
			return this.client;
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		Assert.notNull(applicationEventPublisher, "'applicationEventPublisher' cannot be null");
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * The phase of component auto-start in {@link SmartLifecycle}.
	 * If the custom one is required, note that for the correct behavior it should be less than phase of
	 * {@link AbstractMqttMessageDrivenChannelAdapter} implementations.
	 * The default phase is {@link #DEFAULT_MANAGER_PHASE}.
	 * @return {@link SmartLifecycle} autostart phase
	 * @see #setPhase
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void addCallback(ConnectCallback connectCallback) {
		this.connectCallbacks.add(connectCallback);
	}

	@Override
	public boolean removeCallback(ConnectCallback connectCallback) {
		return this.connectCallbacks.remove(connectCallback);
	}

	public boolean isRunning() {
		this.lock.lock();
		try {
			return this.client != null;
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Set the phase of component autostart in {@link SmartLifecycle}.
	 * If the custom one is required, note that for the correct behavior it should be less than phase of
	 * {@link AbstractMqttMessageDrivenChannelAdapter} implementations.
	 * @see #getPhase
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

}
