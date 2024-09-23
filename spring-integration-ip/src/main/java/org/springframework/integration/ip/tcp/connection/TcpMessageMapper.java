/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

/**
 * Maps incoming data from a {@link TcpConnection} to a {@link Message}.
 * If StringToBytes is true (default),
 * payloads of type String are converted to a byte[] using the supplied
 * charset (UTF-8 by default).
 * Inbound messages include headers representing the remote end of the
 * connection as well as a connection id that can be used by a {@link TcpSender}
 * to correlate which connection to send a reply. If applySequence is set, adds
 * standard correlationId/sequenceNumber headers allowing for downstream (unbounded)
 * resequencing.
 * *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 *
 */
public class TcpMessageMapper implements
		InboundMessageMapper<TcpConnection>,
		OutboundMessageMapper<Object>,
		BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR final

	private String charset = "UTF-8";

	private boolean stringToBytes = true;

	private boolean applySequence = false;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	private String contentType = "application/octet-stream;charset=" + this.charset;

	private boolean addContentTypeHeader;

	private BeanFactory beanFactory;

	private BytesMessageMapper bytesMessageMapper;

	/**
	 * Set the charset to use when converting outbound String messages to {@code byte[]}.
	 * @param charset the charset to set
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * Sets whether outbound String payloads are to be converted
	 * to byte[]. Default is true.
	 * Ignored if a {@link BytesMessageMapper} is provided.
	 * @param stringToBytes The stringToBytes to set.
	 * @see #setBytesMessageMapper(BytesMessageMapper)
	 */
	public void setStringToBytes(boolean stringToBytes) {
		this.stringToBytes = stringToBytes;
	}

	/**
	 * @param applySequence The applySequence to set.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	/**
	 * Set the content type header value to add to inbound messages when
	 * {@link #setAddContentTypeHeader(boolean) addContentTypeHeader} is true.
	 * Default {@code application/octet-stream;charset=UTF-8}. This default is <b>not</b>
	 * modified by {@link #setCharset(String)}.
	 * @param contentType the content type header value to set.
	 * @since 4.3
	 * @see #setAddContentTypeHeader(boolean)
	 * @see TcpMessageMapper#setCharset(String)
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' cannot be null");
		try {
			MimeType.valueOf(contentType);
		}
		catch (InvalidMimeTypeException e) {
			throw new IllegalArgumentException("'contentType' could not be parsed", e);
		}
		this.contentType = contentType;
	}

	/**
	 * Set to true to add a content type header; default false.
	 * @param addContentTypeHeader true to add a content type header.
	 * @since 4.3
	 * @see #setContentType(String)
	 */
	public void setAddContentTypeHeader(boolean addContentTypeHeader) {
		this.addContentTypeHeader = addContentTypeHeader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Set a {@link BytesMessageMapper} to use when mapping byte[].
	 * {@link #setStringToBytes(boolean)} is ignored when a {@link BytesMessageMapper}
	 * is provided.
	 * @param bytesMessageMapper the mapper.
	 * @since 5.0
	 * @see #setStringToBytes(boolean)
	 */
	public void setBytesMessageMapper(BytesMessageMapper bytesMessageMapper) {
		this.bytesMessageMapper = bytesMessageMapper;
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

	@SuppressWarnings("unchecked")
	@Override
	public Message<?> toMessage(TcpConnection connection, @Nullable Map<String, Object> headers) {
		Message<Object> message = null;
		Object payload = connection.getPayload();
		if (payload != null) {
			AbstractIntegrationMessageBuilder<Object> messageBuilder;
			if (this.bytesMessageMapper != null && payload instanceof byte[]) {
				messageBuilder = (AbstractIntegrationMessageBuilder<Object>) getMessageBuilderFactory()
						.fromMessage(this.bytesMessageMapper.toMessage((byte[]) payload));
			}
			else {
				messageBuilder = getMessageBuilderFactory()
						.withPayload(payload);
			}

			MessageHeaders messageHeaders = new MutableMessageHeaders(null);

			addStandardHeaders(connection, messageHeaders);
			addCustomHeaders(connection, messageHeaders);

			message = messageBuilder
					.copyHeaders(messageHeaders)
					.copyHeadersIfAbsent(headers)
					.build();
		}
		else {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("Null payload from connection " + connection.getConnectionId());
			}
		}
		return message;
	}

	protected final void addStandardHeaders(TcpConnection connection, MessageHeaders messageHeaders) {
		String connectionId = connection.getConnectionId();

		messageHeaders.put(IpHeaders.HOSTNAME, connection.getHostName());
		messageHeaders.put(IpHeaders.IP_ADDRESS, connection.getHostAddress());
		messageHeaders.put(IpHeaders.REMOTE_PORT, connection.getPort());
		messageHeaders.put(IpHeaders.CONNECTION_ID, connectionId);

		SocketInfo socketInfo = connection.getSocketInfo();
		if (socketInfo != null) {
			messageHeaders.put(IpHeaders.LOCAL_ADDRESS, socketInfo.getLocalAddress());
		}
		if (this.applySequence) {
			messageHeaders.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, connectionId);
			messageHeaders.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
					connection.incrementAndGetConnectionSequence());
		}
		if (this.addContentTypeHeader) {
			messageHeaders.put(MessageHeaders.CONTENT_TYPE, this.contentType);
		}
	}

	protected final void addCustomHeaders(TcpConnection connection, MessageHeaders messageHeaders) {
		Map<String, ?> customHeaders = supplyCustomHeaders(connection);
		if (customHeaders != null) {
			customHeaders.forEach(messageHeaders::putIfAbsent);
		}
	}

	/**
	 * Override to provide additional headers. The standard headers cannot be overridden
	 * and any such headers will be ignored if provided in the result.
	 * @param connection the connection.
	 * @return A Map of {@code <String, ?>} headers to be added to the message.
	 */
	@Nullable
	protected Map<String, ?> supplyCustomHeaders(TcpConnection connection) {
		return null;
	}

	@Override
	public Object fromMessage(Message<?> message) {
		if (this.bytesMessageMapper != null) {
			return this.bytesMessageMapper.fromMessage(message);
		}
		if (this.stringToBytes) {
			return getPayloadAsBytes(message);
		}
		return message.getPayload();
	}

	/**
	 * Extracts the payload as a byte array.
	 */
	private byte[] getPayloadAsBytes(Message<?> message) {
		byte[] bytes = null;
		Object payload = message.getPayload();
		if (payload instanceof byte[] castBytes) {
			bytes = castBytes;
		}
		else if (payload instanceof String string) {
			try {
				bytes = string.getBytes(this.charset);
			}
			catch (UnsupportedEncodingException e) {
				throw new UncheckedIOException(e);
			}
		}
		else {
			throw new IllegalArgumentException(
					"When using a byte array serializer, the socket mapper expects " +
							"either a byte array or String payload, but received: " + payload.getClass());
		}
		return bytes;
	}

}
