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

package org.springframework.integration.zeromq.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;

/**
 * Default implementation for mapping to/from Messages.
 *
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
public class DefaultZeromqMessageConverter implements ZeromqMessageConverter, BeanFactoryAware {

	private final String charset;

	private BytesMessageMapper bytesMessageMapper;

	private volatile boolean payloadAsBytes = false;

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile boolean messageBuilderFactorySet;

	/**
	 * Construct a converter with default options (charset=UTF-8).
	 */
	public DefaultZeromqMessageConverter() {
		this("UTF-8");
	}

	/**
	 * Construct a converter to create outbound messages with the
	 * UTF-8 charset for converting outbound String payloads to
	 * {@code byte[]} and inbound {@code byte[]} to String (unless
	 * {@link #setPayloadAsBytes(boolean) payloadAdBytes} is true).
	 * @param charset the charset.
	 */
	public DefaultZeromqMessageConverter(String charset) {
		this.charset = charset;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
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
	 *
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
	 * @since 5.0.1
	 * @see #setPayloadAsBytes(boolean)
	 */
	public void setBytesMessageMapper(BytesMessageMapper bytesMessageMapper) {
		this.bytesMessageMapper = bytesMessageMapper;
	}

	@Override
	public Message<?> toMessage(Object zmqMessage, MessageHeaders headers) {
		Assert.isInstanceOf(byte[].class, zmqMessage,
				() -> "This converter can only convert an 'ZeromqMessage'; received: "
						+ byte[].class.getName());
		return toMessage(null, (byte[]) zmqMessage);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Message<?> toMessage(String topic, byte[] zmqMessage) {
		try {
			AbstractIntegrationMessageBuilder<Object> messageBuilder;
			if (this.bytesMessageMapper != null) {
				messageBuilder = (AbstractIntegrationMessageBuilder<Object>) getMessageBuilderFactory()
						.fromMessage(this.bytesMessageMapper.toMessage(zmqMessage));
			}
			else {
				messageBuilder = getMessageBuilderFactory()
						.withPayload(zmqBytesToPayload(zmqMessage));
			}
			if (topic != null) {
				messageBuilder.setHeader("TOPIC", topic);
			}
			return messageBuilder.build();
		}
		catch (Exception e) {
			throw new MessageConversionException("failed to convert object to Message", e);
		}
	}

	@Override
	public byte[] fromMessage(Message<?> message, Class<?> targetClass) {
		byte[] payloadBytes = messageToZmqBytes(message);
		byte[] zmqMessage = payloadBytes;
		return zmqMessage;
	}

	/**
	 * Subclasses can override this method to convert the byte[] to a payload.
	 * The default implementation creates a String (default) or byte[].
	 *
	 * @param zmqMessage The inbound message.
	 * @return The payload for the Spring integration message
	 * @throws Exception Any.
	 */
	protected Object zmqBytesToPayload(byte[] zmqMessage) throws Exception {
		if (this.payloadAsBytes) {
			return zmqMessage;
		}
		else {
			return new String(zmqMessage, this.charset);
		}
	}

	/**
	 * Subclasses can override this method to convert the payload to a byte[].
	 * The default implementation accepts a byte[] or String payload.
	 * If a {@link BytesMessageMapper} is provided, conversion to byte[]
	 * is delegated to it, so any payload that it can handle is supported.
	 *
	 * @param message The outbound Message.
	 * @return The byte[] which will become the payload of the ZMQ Message.
	 */
	protected byte[] messageToZmqBytes(Message<?> message) {
		if (this.bytesMessageMapper != null) {
			try {
				return this.bytesMessageMapper.fromMessage(message);
			}
			catch (Exception e) {
				throw new MessageHandlingException(message, "Failed to map outbound message", e);
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
}
