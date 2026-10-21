/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import java.util.concurrent.Executor;

import com.hivemq.client.mqtt.datatypes.MqttQos;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.mqtt.client.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.springframework.messaging.converter.SmartMessageConverter;

/**
 * Common options for MQTT inbound adapters.
 *
 * @param <S> the concrete spec type.
 * @param <A> the adapter type.
 *
 * @author YeongJae Min
 *
 * @since 7.3
 */
public abstract class MqttMessageDrivenChannelAdapterSpec<
		S extends MqttMessageDrivenChannelAdapterSpec<S, A>, A extends AbstractMqttMessageDrivenChannelAdapter<?>>
		extends MessageProducerSpec<S, A> {

	/**
	 * Construct a spec for the provided adapter.
	 * @param adapter the adapter.
	 * @since 7.3
	 */
	protected MqttMessageDrivenChannelAdapterSpec(A adapter) {
		super(adapter);
	}

	/**
	 * Set the QoS for topic-derived subscriptions.
	 * Explicit subscriptions retain their own QoS.
	 * @param qos the qos.
	 * @return the spec.
	 * @since 7.3
	 */
	public S qos(MqttQos qos) {
		this.target.setQos(qos);
		return _this();
	}

	/**
	 * Set whether incoming publishes are acknowledged manually.
	 * @param manualAck the manualAck.
	 * @return the spec.
	 * @since 7.3
	 */
	public S manualAck(boolean manualAck) {
		this.target.setManualAck(manualAck);
		return _this();
	}

	/**
	 * Set the executor for incoming message callbacks.
	 * @param executor the executor.
	 * @return the spec.
	 * @since 7.3
	 */
	public S executor(Executor executor) {
		this.target.setExecutor(executor);
		return _this();
	}

	/**
	 * Set the converted payload type; the default is {@code byte[].class}.
	 * Use the native publish type to receive the entire MQTT publish.
	 * @param payloadType the payloadType.
	 * @return the spec.
	 * @since 7.3
	 */
	public S payloadType(Class<?> payloadType) {
		this.target.setPayloadType(payloadType);
		return _this();
	}

	/**
	 * Set the converter for incoming payloads.
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @since 7.3
	 */
	public S messageConverter(SmartMessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

}
