/*
 * Copyright 2002-2019 the original author or authors.
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
