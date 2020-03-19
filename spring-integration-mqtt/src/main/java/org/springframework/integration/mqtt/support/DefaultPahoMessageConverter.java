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

package org.springframework.integration.mqtt.support;

import java.nio.charset.Charset;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;


/**
 * Default implementation for mapping to/from Messages.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class DefaultPahoMessageConverter implements MqttMessageConverter, BeanFactoryAware {

	private final Charset charset;

	private final int defaultQos;

	private final MessageProcessor<Integer> qosProcessor;

	private final boolean defaultRetained;

	private final MessageProcessor<Boolean> retainedProcessor;

	private BytesMessageMapper bytesMessageMapper;

	private volatile boolean payloadAsBytes = false;

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;


	/**
	 * Construct a converter with default options (qos=0, retain=false, charset=UTF-8).
	 */
	public DefaultPahoMessageConverter() {
		this(0, false);
	}

	/**
	 * Construct a converter to create outbound messages with the supplied default qos and
	 * retain settings and a UTF-8 charset for converting outbound String payloads to
	 * {@code byte[]} and inbound {@code byte[]} to String (unless
	 * {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 * @param defaultQos the default qos.
	 * @param defaultRetained the default retained.
	 */
	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetained) {
		this(defaultQos, defaultRetained, "UTF-8");
	}

	/**
	 * Construct a converter with default options (qos=0, retain=false) and
	 * the supplied charset.
	 * @param charset the charset used to convert outbound String payloads to {@code byte[]} and inbound
	 * {@code byte[]} to String (unless {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 * @since 4.1.2
	 */
	public DefaultPahoMessageConverter(String charset) {
		this(0, false, charset);
	}

	/**
	 * Construct a converter to create outbound messages with the supplied default qos and
	 * retain settings and the supplied charset.
	 * @param defaultQos the default qos.
	 * @param defaultRetained the default retained.
	 * @param charset the charset used to convert outbound String payloads to
	 * {@code byte[]} and inbound {@code byte[]} to String (unless
	 * {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 */
	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetained, String charset) {
		this(defaultQos, MqttMessageConverter.defaultQosProcessor(), defaultRetained,
				MqttMessageConverter.defaultRetainedProcessor(), charset);
	}

	/**
	 * Construct a converter to create outbound messages with the supplied default qos and
	 * retained message processors and a UTF-8 charset for converting outbound String payloads to
	 * {@code byte[]} and inbound {@code byte[]} to String (unless
	 * {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 * @param defaultQos the default qos.
	 * @param qosProcessor a message processor to determine the qos.
	 * @param defaultRetained the default retained.
	 * @param retainedProcessor a message processor to determine the retained flag.
	 * @since 5.0
	 */
	public DefaultPahoMessageConverter(int defaultQos, MessageProcessor<Integer> qosProcessor, boolean defaultRetained,
			MessageProcessor<Boolean> retainedProcessor) {

		this(defaultQos, qosProcessor, defaultRetained, retainedProcessor, "UTF-8");
	}

	/**
	 * Construct a converter to create outbound messages with the supplied default qos and
	 * retain settings and the supplied charset.
	 * @param defaultQos the default qos.
	 * @param qosProcessor a message processor to determine the qos.
	 * @param defaultRetained the default retained.
	 * @param retainedProcessor a message processor to determine the retained flag.
	 * @param charset the charset used to convert outbound String payloads to
	 * {@code byte[]} and inbound {@code byte[]} to String (unless
	 * {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 * @since 5.0
	 */
	public DefaultPahoMessageConverter(int defaultQos, MessageProcessor<Integer> qosProcessor, boolean defaultRetained,
			MessageProcessor<Boolean> retainedProcessor, String charset) {

		Assert.notNull(qosProcessor, "'qosProcessor' cannot be null");
		Assert.notNull(retainedProcessor, "'retainedProcessor' cannot be null");
		this.defaultQos = defaultQos;
		this.qosProcessor = qosProcessor;
		this.defaultRetained = defaultRetained;
		this.retainedProcessor = retainedProcessor;
		this.charset = Charset.forName(charset);
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	/**
	 * True if the converter should not convert the message payload to a String.
	 * Ignored if a {@link BytesMessageMapper} is provided.
	 * @param payloadAsBytes The payloadAsBytes to set.
	 * @see #setBytesMessageMapper(BytesMessageMapper)
	 */
	public void setPayloadAsBytes(boolean payloadAsBytes) {
		this.payloadAsBytes = payloadAsBytes;
	}

	public boolean isPayloadAsBytes() {
		return this.payloadAsBytes;
	}

	/**
	 * Set a {@link BytesMessageMapper} to use when mapping byte[].
	 * {@link #setPayloadAsBytes(boolean)} is ignored when a {@link BytesMessageMapper}
	 * is provided.
	 * @param bytesMessageMapper the mapper.
	 * @since 5.0
	 * @see #setPayloadAsBytes(boolean)
	 */
	public void setBytesMessageMapper(BytesMessageMapper bytesMessageMapper) {
		this.bytesMessageMapper = bytesMessageMapper;
	}

	@Override
	public Message<?> toMessage(Object mqttMessage, MessageHeaders headers) {
		Assert.isInstanceOf(MqttMessage.class, mqttMessage,
				() -> "This converter can only convert an 'MqttMessage'; received: "
						+ mqttMessage.getClass().getName());
		return toMessage(null, (MqttMessage) mqttMessage);
	}

	@Override
	public Message<?> toMessage(String topic, MqttMessage mqttMessage) {
		return toMessageBuilder(topic, mqttMessage).build();
	}

	@Override
	public AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage) {
		try {
			AbstractIntegrationMessageBuilder<?> messageBuilder;
			if (this.bytesMessageMapper != null) {
				messageBuilder =
						getMessageBuilderFactory()
								.fromMessage(this.bytesMessageMapper.toMessage(mqttMessage.getPayload()));
			}
			else {
				messageBuilder =
						getMessageBuilderFactory()
								.withPayload(mqttBytesToPayload(mqttMessage));
			}
			messageBuilder
					.setHeader(MqttHeaders.ID, mqttMessage.getId())
					.setHeader(MqttHeaders.RECEIVED_QOS, mqttMessage.getQos())
					.setHeader(MqttHeaders.DUPLICATE, mqttMessage.isDuplicate())
					.setHeader(MqttHeaders.RECEIVED_RETAINED, mqttMessage.isRetained());
			if (topic != null) {
				messageBuilder.setHeader(MqttHeaders.RECEIVED_TOPIC, topic);
			}
			return messageBuilder;
		}
		catch (Exception e) {
			throw new MessageConversionException("failed to convert object to Message", e);
		}
	}

	@Override
	public MqttMessage fromMessage(Message<?> message, Class<?> targetClass) {
		byte[] payloadBytes = messageToMqttBytes(message);
		MqttMessage mqttMessage = new MqttMessage(payloadBytes);
		Integer qos = this.qosProcessor.processMessage(message);
		mqttMessage.setQos(qos == null ? this.defaultQos : qos);
		Boolean retained = this.retainedProcessor.processMessage(message);
		mqttMessage.setRetained(retained == null ? this.defaultRetained : retained);
		return mqttMessage;
	}

	/**
	 * Subclasses can override this method to convert the byte[] to a payload.
	 * The default implementation creates a String (default) or byte[].
	 * @param mqttMessage The inbound message.
	 * @return The payload for the Spring integration message
	 */
	protected Object mqttBytesToPayload(MqttMessage mqttMessage) {
		if (this.payloadAsBytes) {
			return mqttMessage.getPayload();
		}
		else {
			return new String(mqttMessage.getPayload(), this.charset);
		}
	}

	/**
	 * Subclasses can override this method to convert the payload to a byte[].
	 * The default implementation accepts a byte[] or String payload.
	 * If a {@link BytesMessageMapper} is provided, conversion to byte[]
	 * is delegated to it, so any payload that it can handle is supported.
	 * @param message The outbound Message.
	 * @return The byte[] which will become the payload of the MQTT Message.
	 */
	protected byte[] messageToMqttBytes(Message<?> message) {
		if (this.bytesMessageMapper != null) {
			try {
				return this.bytesMessageMapper.fromMessage(message);
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to map outbound message", e);
			}
		}
		else {
			Object payload = message.getPayload();
			Assert.isTrue(payload instanceof byte[] || payload instanceof String,
					() -> "This default converter can only handle 'byte[]' or 'String' payloads; consider adding a "
							+ "transformer to your flow definition, or provide a BytesMessageMapper, "
							+ "or subclass this converter for "
							+ payload.getClass().getName() + " payloads");
			byte[] payloadBytes;
			if (payload instanceof String) {
				payloadBytes = ((String) payload).getBytes(this.charset);
			}
			else {
				payloadBytes = (byte[]) payload;
			}
			return payloadBytes;
		}
	}

}
