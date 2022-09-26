/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * Utility methods used during message mapping.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public final class MappingUtils {

	private MappingUtils() {
	}

	/**
	 * Map an o.s.m.Message to an o.s.a.core.Message. When using a
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

		return doMapMessage(requestMessage, converter, headerMapper, defaultDeliveryMode, headersMappedLast, false);
	}

	/**
	 * Map a reply o.s.m.Message to an o.s.a.core.Message. When using a
	 * {@link ContentTypeDelegatingMessageConverter}, {@link AmqpHeaders#CONTENT_TYPE} and
	 * {@link MessageHeaders#CONTENT_TYPE} will be used for the selection, with the AMQP
	 * header taking precedence.
	 * @param replyMessage the reply message.
	 * @param converter the message converter to use.
	 * @param headerMapper the header mapper to use.
	 * @param defaultDeliveryMode the default delivery mode.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 * @return the mapped Message.
	 * @since 5.1.9
	 */
	public static org.springframework.amqp.core.Message mapReplyMessage(Message<?> replyMessage,
			MessageConverter converter, AmqpHeaderMapper headerMapper,
			@Nullable MessageDeliveryMode defaultDeliveryMode, boolean headersMappedLast) {

		return doMapMessage(replyMessage, converter, headerMapper, defaultDeliveryMode, headersMappedLast, true);
	}

	private static org.springframework.amqp.core.Message doMapMessage(Message<?> message,
			MessageConverter converter, AmqpHeaderMapper headerMapper,
			@Nullable MessageDeliveryMode defaultDeliveryMode, boolean headersMappedLast, boolean reply) {

		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage = mapMessage(message, converter, headerMapper,
				headersMappedLast, reply, amqpMessageProperties);
		checkDeliveryMode(message, amqpMessageProperties, defaultDeliveryMode);
		return amqpMessage;
	}

	/**
	 * Map a reply o.s.m.Message to an o.s.a.core.Message. When using a
	 * {@link ContentTypeDelegatingMessageConverter}, {@link AmqpHeaders#CONTENT_TYPE} and
	 * {@link MessageHeaders#CONTENT_TYPE} will be used for the selection, with the AMQP
	 * header taking precedence.
	 * @param replyMessage the reply message.
	 * @param converter the message converter to use.
	 * @param headerMapper the header mapper to use.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 * @return the mapped Message.
	 * @since 6.0
	 */
	public static org.springframework.amqp.core.Message mapMessage(Message<?> message, MessageConverter converter,
			AmqpHeaderMapper headerMapper, boolean headersMappedLast, boolean reply,
			MessageProperties amqpMessageProperties) {

		org.springframework.amqp.core.Message amqpMessage;
		if (!headersMappedLast) {
			mapHeaders(message.getHeaders(), amqpMessageProperties, headerMapper, reply);
		}
		if (converter instanceof ContentTypeDelegatingMessageConverter && headersMappedLast) {
			String contentType = contentTypeAsString(message.getHeaders());
			if (contentType != null) {
				amqpMessageProperties.setContentType(contentType);
			}
		}
		amqpMessage = converter.toMessage(message.getPayload(), amqpMessageProperties);
		if (headersMappedLast) {
			mapHeaders(message.getHeaders(), amqpMessageProperties, headerMapper, reply);
		}
		return amqpMessage;
	}

	private static void mapHeaders(MessageHeaders messageHeaders, MessageProperties amqpMessageProperties,
			AmqpHeaderMapper headerMapper, boolean reply) {

		if (reply) {
			headerMapper.fromHeadersToReply(messageHeaders, amqpMessageProperties);
		}
		else {
			headerMapper.fromHeadersToRequest(messageHeaders, amqpMessageProperties);
		}
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
			@Nullable MessageDeliveryMode defaultDeliveryMode) {
		if (defaultDeliveryMode != null &&
				requestMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE) == null) {
			messageProperties.setDeliveryMode(defaultDeliveryMode);
		}
	}

}
