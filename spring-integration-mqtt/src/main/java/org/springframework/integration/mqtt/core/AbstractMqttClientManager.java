/*
 * Copyright 2022-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.mqtt.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.springframework.util.Assert;

/**
 * @param <T> MQTT client type
 * @param <C> MQTT connection options type (v5 or v3)
 *
 * @author Artem Vozhdayenko
 *
 * @since 6.0
 */
public abstract class AbstractMqttClientManager<T, C> implements ClientManager<T, C>, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR

	private final Set<ConnectCallback> connectCallbacks;

	private final String clientId;

	private boolean manualAcks;

	private ApplicationEventPublisher applicationEventPublisher;

	private String url;

	private volatile T client;

	private String beanName;

	AbstractMqttClientManager(String clientId) {
		Assert.notNull(clientId, "'clientId' is required");
		this.clientId = clientId;
		this.connectCallbacks = Collections.synchronizedSet(new HashSet<>());
	}

	protected void setManualAcks(boolean manualAcks) {
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

	protected synchronized void setClient(T client) {
		this.client = client;
	}

	protected Set<ConnectCallback> getCallbacks() {
		return Collections.unmodifiableSet(this.connectCallbacks);
	}

	@Override
	public boolean isManualAcks() {
		return this.manualAcks;
	}

	@Override
	public T getClient() {
		return this.client;
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
	 * The phase of component autostart in {@link SmartLifecycle}.
	 * If the custom one is required, note that for the correct behavior it should be less than phase of
	 * {@link AbstractMqttMessageDrivenChannelAdapter} implementations.
	 * @return {@link SmartLifecycle} autostart phase
	 */
	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void addCallback(ConnectCallback connectCallback) {
		this.connectCallbacks.add(connectCallback);
	}

	@Override
	public boolean removeCallback(ConnectCallback connectCallback) {
		return this.connectCallbacks.remove(connectCallback);
	}

	public synchronized boolean isRunning() {
		return this.client != null;
	}

}
