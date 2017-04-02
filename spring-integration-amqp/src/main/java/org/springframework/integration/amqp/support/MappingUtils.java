/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * Utility methods used during message mapping.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public final class MappingUtils {

	private MappingUtils() {
		super();
	}

	/**
	 * Map an o.s.Message to an o.s.a.core.Message. When using a
	 * {@link ContentTypeDelegatingMessageConverter}, {@link AmqpHeaders#CONTENT_TYPE} and
	 * {@link MessageHeaders#CONTENT_TYPE} will be used for the selection, with the AMQP
	 * header taking precedence.
	 * @param requestMessage the request message.
	 * @param converter the message converter to use.
	 * @param headerMapper the header mapper to use.
	 * @param defaultDeliveryMode the default delivery mode.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 * @return the mapped Message.
	 */
	public static org.springframework.amqp.core.Message mapMessage(Message<?> requestMessage,
			MessageConverter converter, AmqpHeaderMapper headerMapper, MessageDeliveryMode defaultDeliveryMode,
			boolean headersMappedLast) {
		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage;
		if (!headersMappedLast) {
			headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), amqpMessageProperties);
		}
		if (converter instanceof ContentTypeDelegatingMessageConverter && headersMappedLast) {
			String contentType = contentTypeAsString(requestMessage.getHeaders());
			if (contentType != null) {
				amqpMessageProperties.setContentType(contentType);
			}
		}
		amqpMessage = converter.toMessage(requestMessage.getPayload(), amqpMessageProperties);
		if (headersMappedLast) {
			headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), amqpMessageProperties);
		}
		checkDeliveryMode(requestMessage, amqpMessageProperties, defaultDeliveryMode);
		return amqpMessage;
	}

	private static String contentTypeAsString(MessageHeaders headers) {
		Object contentType = headers.get(AmqpHeaders.CONTENT_TYPE);
		if (contentType instanceof MimeType) {
			contentType = contentType.toString();
		}
		if (contentType instanceof String) {
			return (String) contentType;
		}
		else if (contentType != null) {
			throw new IllegalArgumentException(AmqpHeaders.CONTENT_TYPE
					+ " header must be a MimeType or String, found: " + contentType.getClass().getName());
		}
		return null;
	}

	/**
	 * Check the delivery mode and update with the default if not already present.
	 * @param requestMessage the request message.
	 * @param messageProperties the mapped message properties.
	 * @param defaultDeliveryMode the default delivery mode.
	 */
	public static void checkDeliveryMode(Message<?> requestMessage, MessageProperties messageProperties,
			MessageDeliveryMode defaultDeliveryMode) {
		if (defaultDeliveryMode != null &&
				requestMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE) == null) {
			messageProperties.setDeliveryMode(defaultDeliveryMode);
		}
	}

}
