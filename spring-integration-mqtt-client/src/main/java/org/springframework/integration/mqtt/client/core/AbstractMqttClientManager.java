/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientBuilder;
import com.hivemq.client.mqtt.MqttClientConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;

/**
 * Abstract class for MQTT client managers which can be a base for any common v3/v5 client manager implementation.
 * Contains some basic utility and implementation-agnostic fields and methods.
 *
 * @param <T> MQTT client type
 * @param <C> MQTT client config
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public abstract class AbstractMqttClientManager<T extends MqttClient, C extends MqttClientConfig>
		implements ClientManager<T>, InitializingBean, SmartLifecycle, ApplicationEventPublisherAware,
		MqttClientConnectedListener {

	/**
	 * The default phase of this client manager auto-start in {@link SmartLifecycle}.
	 */
	private static final int DEFAULT_MANAGER_PHASE = 0;

	protected final LogAccessor logger = new LogAccessor(getClass());

	protected final Lock lock = new ReentrantLock();

	protected final Set<ConnectCallback> connectCallbacks = Collections.synchronizedSet(new HashSet<>());

	protected final C mqttClientConfig;

	private int phase = DEFAULT_MANAGER_PHASE;

	@SuppressWarnings("NullAway.Init")
	private T mqttClient;

	@SuppressWarnings("NullAway.Init")
	private ApplicationEventPublisher applicationEventPublisher;

	protected AbstractMqttClientManager(C mqttClientConfig) {
		if (mqttClientConfig.getAutomaticReconnect().isEmpty()) {
			this.logger.info("If this `ClientManager` is used from message-driven channel adapters, " +
					"it is recommended to enable 'automaticReconnect' when build the 'mqttClientConfig'. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
		this.mqttClientConfig = mqttClientConfig;
	}

	@Override
	public void afterPropertiesSet() {
		this.mqttClient = buildClient();
	}

	/**
	 * Build the mqttClient using the supplied {@link MqttClientConfig}.
	 * @return the mqttClient
	 */
	protected abstract T buildClient();

	/**
	 * Create a base {@link MqttClientBuilder} populated with the supplied {@link MqttClientConfig}.
	 * @param inputConfig the MqttClientConfig
	 * @return the MqttClientBuilder
	 */
	protected MqttClientBuilder createBaseClientBuilder(MqttClientConfig inputConfig) {
		MqttClientBuilder builder = MqttClient.builder();

		if (inputConfig.getClientIdentifier().isPresent()) {
			builder = builder.identifier(inputConfig.getClientIdentifier().get());
		}

		builder = builder
				.serverAddress(inputConfig.getServerAddress())
				.serverHost(inputConfig.getServerHost())
				.serverPort(inputConfig.getServerPort())
				.sslConfig(inputConfig.getSslConfig().orElse(null))
				.webSocketConfig(inputConfig.getWebSocketConfig().orElse(null))
				.transportConfig(inputConfig.getTransportConfig())
				.executorConfig(inputConfig.getExecutorConfig())
				// automaticReconnect(if any) will be auto registered in disconnectedListener.
				// so have to skip appending this in the new built disconnectedListener list
				.automaticReconnect(inputConfig.getAutomaticReconnect().orElse(null));

		for (MqttClientConnectedListener connectedListener : inputConfig.getConnectedListeners()) {
			builder = builder.addConnectedListener(connectedListener);
		}

		for (MqttClientDisconnectedListener disconnectedListener : inputConfig.getDisconnectedListeners()) {
			if (disconnectedListener instanceof MqttClientAutoReconnect) {
				continue;
			}
			builder = builder.addDisconnectedListener(disconnectedListener);
		}

		return builder;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	/**
	 * Set the phase of component autostart in {@link SmartLifecycle}.
	 * If the custom one is required, note that for the correct behavior it should be less than phase of
	 * {@code AbstractMqttMessageDrivenChannelAdapter} implementations.
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
	 * {@code AbstractMqttMessageDrivenChannelAdapter} implementations.
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
		return isConnected();
	}

	@Override
	public boolean isConnected() {
		return this.mqttClient.getState().isConnected();
	}

}
