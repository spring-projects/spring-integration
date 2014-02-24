/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class MessageConvertingTcpMessageMapper extends TcpMessageMapper {

	private final MessageConverter messageConverter;

	public MessageConvertingTcpMessageMapper(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messasgeConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	@Override
	public Message<?> toMessage(TcpConnection connection) throws Exception {
		Object data = connection.getPayload();
		if (data != null) {
			Message<?> message = this.messageConverter.toMessage(data, null);
			AbstractIntegrationMessageBuilder<?> messageBuilder = this.getMessageBuilderFactory().fromMessage(message);
			this.addStandardHeaders(connection, messageBuilder);
			this.addCustomHeaders(connection, messageBuilder);
			return messageBuilder.build();
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("Null payload from connection " + connection.getConnectionId());
			}
			return null;
		}
	}

	@Override
	public Object fromMessage(Message<?> message) throws Exception {
		return this.messageConverter.fromMessage(message, Object.class);
	}

}
