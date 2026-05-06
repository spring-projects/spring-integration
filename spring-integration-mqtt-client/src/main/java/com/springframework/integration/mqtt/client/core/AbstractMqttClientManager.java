/*
 * Copyright 2026-present the original author or authors.
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

package com.springframework.integration.mqtt.client.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hivemq.client.mqtt.MqttClient;
import com.springframework.integration.mqtt.client.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT client managers which can be a base for any common v3/v5 client manager implementation.
 * Contains some basic utility and implementation-agnostic fields and methods.
 *
 * @param <T> MQTT client type
 * @param <B> MQTT client builder
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public abstract class AbstractMqttClientManager<T extends MqttClient, B>
		implements MqttClientManager<T>, ApplicationEventPublisherAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final Lock lock = new ReentrantLock();

	private int phase = DEFAULT_MANAGER_PHASE;

	protected final Set<ConnectCallback> connectCallbacks = Collections.synchronizedSet(new HashSet<>());

	protected final B mqttClientBuilder;

	@SuppressWarnings("NullAway.Init")
	protected T mqttClient;

	@SuppressWarnings("NullAway.Init")
	protected ApplicationEventPublisher applicationEventPublisher;

	protected AbstractMqttClientManager(B mqttClientBuilder) {
		this.mqttClientBuilder = mqttClientBuilder;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		Assert.notNull(applicationEventPublisher, "'applicationEventPublisher' cannot be null");
		this.applicationEventPublisher = applicationEventPublisher;
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

	@Override
	public void addCallback(ConnectCallback connectCallback) {
		this.connectCallbacks.add(connectCallback);
	}

	@Override
	public boolean removeCallback(ConnectCallback connectCallback) {
		return this.connectCallbacks.remove(connectCallback);
	}

	@Override
	public T getClient() {
		return this.mqttClient;
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
	public boolean isRunning() {
		return true;
	}

	@Override
	public boolean isConnected() {
		this.lock.lock();
		try {
			return this.mqttClient.getState().isConnected();
		}
		finally {
			this.lock.unlock();
		}
	}

}
