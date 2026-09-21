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

package org.springframework.integration.mqtt.client.inbound;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT Message-Driven Channel Adapters.
 *
 * @param <T> MQTT Client type
 *
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public abstract class AbstractMqttMessageDrivenChannelAdapter<T extends MqttClient>
		extends MessageProducerSupport implements ApplicationEventPublisherAware, ClientManager.ConnectCallback {

	protected final ClientManager<T> mqttClientManager;

	@SuppressWarnings("NullAway.Init")
	private T mqttClient;

	protected final AtomicBoolean isSubscribing = new AtomicBoolean();

	protected final AtomicBoolean isSubscribed = new AtomicBoolean();

	private String @Nullable [] topics;

	private MqttQos qos = MqttQos.AT_LEAST_ONCE;

	private boolean manualAck;

	private @Nullable Executor executor;

	@SuppressWarnings("NullAway.Init")
	private ApplicationEventPublisher applicationEventPublisher;

	@SuppressWarnings("NullAway.Init")
	protected SmartMessageConverter messageConverter;

	private Class<?> payloadType = byte[].class;

	protected AbstractMqttMessageDrivenChannelAdapter(ClientManager<T> mqttClientManager) {
		this.mqttClientManager = mqttClientManager;
	}

	protected AbstractMqttMessageDrivenChannelAdapter(ClientManager<T> mqttClientManager, String... topics) {
		this.mqttClientManager = mqttClientManager;
		this.topics = topics;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.mqttClient = this.mqttClientManager.getClient();
		this.mqttClientManager.addCallback(this);
		if (this.messageConverter == null) {
			String messageConverterBeanName = IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME;
			setMessageConverter(getBeanFactory().getBean(messageConverterBeanName, SmartMessageConverter.class));
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		this.mqttClientManager.removeCallback(AbstractMqttMessageDrivenChannelAdapter.this);
	}

	protected T getClient() {
		return this.mqttClient;
	}

	/**
	 * Set the messageConverter to convert the payload to the expected payloadType.
	 * @param messageConverter the messageConverter
	 */
	public void setMessageConverter(SmartMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null.");
		this.messageConverter = messageConverter;
	}

	protected SmartMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	protected String @Nullable [] getTopics() {
		return this.topics;
	}

	/**
	 * Set the QoS for the topic
	 * @param qos The qos value
	 */
	public void setQos(MqttQos qos) {
		Assert.notNull(qos, "'qos' must not be null.");
		this.qos = qos;
	}

	protected MqttQos getQos() {
		return this.qos;
	}

	/**
	 * Set whether the Publish messages are acknowledged manually.
	 * @param manualAck true for manual ack.
	 */
	public void setManualAck(boolean manualAck) {
		this.manualAck = manualAck;
	}

	protected boolean isManualAck() {
		return this.manualAck;
	}

	/**
	 * Set the executor where the message callback is executed on.
	 * @param executor the executor.
	 */
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null.");
		this.executor = executor;
	}

	protected @Nullable Executor getExecutor() {
		return this.executor;
	}

	/**
	 * Set the type of the target message payload to produce after conversion from MQTT message.
	 * Defaults to {@code byte[].class}. Can be set to {@code Mqtt5Publish} for v5 or {@code Mqtt3Publish}
	 * to produce the whole MQTT message as a payload.
	 * @param payloadType the expected payload type to convert MQTT message to.
	 */
	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null.");
		this.payloadType = payloadType;
	}

	protected Class<?> getPayloadType() {
		return this.payloadType;
	}

	/**
	 * Return whether the mqttClient isConnected.
	 * @return whether the mqttClient isConnected.
	 */
	public boolean isConnected() {
		return this.mqttClient.getState().isConnected();
	}

	@Override
	public String getComponentType() {
		return "mqtt:inbound-channel-adapter";
	}

}
