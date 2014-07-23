/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.mqtt.support;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
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
 * @since 4.0
 *
 */
public class DefaultPahoMessageConverter implements MqttMessageConverter, BeanFactoryAware {

	private final String charset;

	private final Integer defaultQos;

	private final Boolean defaultRetained;

	private volatile boolean payloadAsBytes = false;

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();


	public DefaultPahoMessageConverter() {
		this (0, false);
	}

	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetain) {
		this(defaultQos, defaultRetain, "UTF-8");
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
	}

	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return messageBuilderFactory;
	}

	/**
	 * True if the converter should not convert the message payload to a String.
	 *
	 * @param payloadAsBytes The payloadAsBytes to set.
	 */
	public void setPayloadAsBytes(boolean payloadAsBytes) {
		this.payloadAsBytes = payloadAsBytes;
	}

	public boolean isPayloadAsBytes() {
		return this.payloadAsBytes;
	}
	
	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetained, String charset) {
		this.defaultQos = defaultQos;
		this.defaultRetained = defaultRetained;
		this.charset = charset;
	}

	@Override
	public Message<?> toMessage(Object mqttMessage, MessageHeaders headers) {
		Assert.isInstanceOf(MqttMessage.class, mqttMessage);
		return toMessage(null, (MqttMessage) mqttMessage);
	}

	@Override
	public Message<?> toMessage(String topic, MqttMessage mqttMessage) {
		try {
			AbstractIntegrationMessageBuilder<Object> messageBuilder = this.messageBuilderFactory
					.withPayload(mqttBytesToPayload(mqttMessage))
					.setHeader(MqttHeaders.QOS, mqttMessage.getQos())
					.setHeader(MqttHeaders.DUPLICATE, mqttMessage.isDuplicate())
					.setHeader(MqttHeaders.RETAINED, mqttMessage.isRetained());
			if (topic != null) {
				messageBuilder.setHeader(MqttHeaders.TOPIC, topic);
			}
			return messageBuilder.build();
		}
		catch (Exception e) {
			throw new MessageConversionException("failed to convert object to Message", e);
		}
	}

	@Override
	public MqttMessage fromMessage(Message<?> message, Class<?> targetClass) {
		byte[] payloadBytes = messageToMqttBytes(message);
		MqttMessage mqttMessage = new MqttMessage(payloadBytes);
		Object header = message.getHeaders().get(MqttHeaders.RETAINED);
		Assert.isTrue(header == null || header instanceof Boolean, MqttHeaders.RETAINED + " header must be Boolean");
		mqttMessage.setRetained(header == null ? this.defaultRetained : (Boolean) header);
		header = message.getHeaders().get(MqttHeaders.QOS);
		Assert.isTrue(header == null || header instanceof Integer, MqttHeaders.QOS + " header must be Integer");
		mqttMessage.setQos(header == null ? this.defaultQos : (Integer) header);
		return mqttMessage;
	}

	/**
	 * Subclasses can override this method to convert the byte[] to a payload.
	 * The default implementation creates a String (default) or byte[].
	 *
	 * @param mqttMessage The inbound message.
	 * @return The payload for the Spring integration message
	 * @throws Exception Any.
	 */
	protected Object mqttBytesToPayload(MqttMessage mqttMessage) throws Exception {
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
	 *
	 * @param message The outbound Message.
	 * @return The byte[] which will become the payload of the MQTT Message.
	 */
	protected byte[] messageToMqttBytes(Message<?> message) {
		Object payload = message.getPayload();
		Assert.isTrue(payload instanceof byte[] || payload instanceof String);
		byte[] payloadBytes;
		if (payload instanceof String) {
			try {
				payloadBytes = ((String) payload).getBytes(this.charset);
			}
			catch (Exception e) {
				throw new MessageConversionException("failed to convert Message to object", e);
			}
		}
		else {
			payloadBytes = (byte[]) payload;
		}
		return payloadBytes;
	}

}
