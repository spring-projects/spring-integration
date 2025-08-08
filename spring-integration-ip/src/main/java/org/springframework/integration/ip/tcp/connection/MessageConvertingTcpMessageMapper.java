/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.util.Map;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @author Artem  Bilan
 *
 * @since 3.0
 *
 */
public class MessageConvertingTcpMessageMapper extends TcpMessageMapper {

	private final MessageConverter messageConverter;

	public MessageConvertingTcpMessageMapper(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	@Override
	public Message<?> toMessage(TcpConnection connection, @Nullable Map<String, Object> headers) {
		Object data = connection.getPayload();
		if (data != null) {

			MessageHeaders messageHeaders = new MutableMessageHeaders(null, MessageHeaders.ID_VALUE_NONE, -1L) {

				private static final long serialVersionUID = 3084692953798643018L;

			};

			addStandardHeaders(connection, messageHeaders);
			addCustomHeaders(connection, messageHeaders);

			if (headers != null) {
				headers.forEach(messageHeaders::putIfAbsent);
			}

			return this.messageConverter.toMessage(data, messageHeaders);
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("Null payload from connection " + connection.getConnectionId());
			}
			return null;
		}
	}

	@Override
	public Object fromMessage(Message<?> message) {
		return this.messageConverter.fromMessage(message, Object.class);
	}

}
