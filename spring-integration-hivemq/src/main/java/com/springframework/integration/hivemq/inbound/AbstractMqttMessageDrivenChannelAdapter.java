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

package com.springframework.integration.hivemq.inbound;

import java.util.concurrent.Executor;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.MessageProducerSupport;
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
		extends MessageProducerSupport implements ApplicationEventPublisherAware {

	@SuppressWarnings("NullAway.Init")
	protected ApplicationEventPublisher applicationEventPublisher;

	@SuppressWarnings("NullAway.Init")
	protected SmartMessageConverter messageConverter;

	protected final T mqttClient;

	protected final String topic;

	protected MqttQos qos = MqttQos.AT_LEAST_ONCE;

	protected boolean manualAck = false;

	protected @Nullable Executor executor;

	protected Class<?> payloadType = byte[].class;

	protected AbstractMqttMessageDrivenChannelAdapter(T mqttClient, String topic) {
		Assert.notNull(mqttClient, "'mqttClient' cannot be null");
		Assert.hasText(topic, "The topic to subscribe cannot be empty string");
		this.mqttClient = mqttClient;
		this.topic = topic;

		if (mqttClient.getConfig().getAutomaticReconnect().isEmpty()) {
			logger.warn("it is recommended to enable 'automaticReconnect' when set this `mqttClient`. " +
					"Otherwise connection check and reconnect should be done manually.");
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.messageConverter == null) {
			String messageConverterBeanName = IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME;
			setMessageConverter(getBeanFactory().getBean(messageConverterBeanName, SmartMessageConverter.class));
		}
	}

	/**
	 * Set the QoS for the topic
	 * @param qos The qos value
	 */
	public void setQos(MqttQos qos) {
		Assert.notNull(qos, "'qos' must not be null.");
		this.qos = qos;
	}

	/**
	 * Set whether the Publish messages are acknowledged manually.
	 * @param manualAck true for manual ack.
	 */
	public void setManualAcknowledgement(boolean manualAck) {
		this.manualAck = manualAck;
	}

	/**
	 * Set the executor where the message callback is executed on.
	 * @param executor the executor.
	 */
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null.");
		this.executor = executor;
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

	/**
	 * Set the messageConverter to convert the payload to the expected payloadType.
	 * @param messageConverter the messageConverter
	 */
	public void setMessageConverter(SmartMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null.");
		this.messageConverter = messageConverter;
	}

	@Override
	public String getComponentType() {
		return "mqtt:inbound-channel-adapter";
	}

}
