/*
 * Copyright 2016 the original author or authors.
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
	 * Map an o.s.Message to an o.s.a.core.Message.
	 * @param requestMessage the request message.
	 * @param converter the message converter to use.
	 * @param headerMapper the header mapper to use.
	 * @param defaultDeliveryMode the default delivery mode.
	 * @return the mapped Message.
	 */
	public static org.springframework.amqp.core.Message mapMessage(Message<?> requestMessage,
			MessageConverter converter, AmqpHeaderMapper headerMapper, MessageDeliveryMode defaultDeliveryMode) {
		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage;
		if (converter instanceof ContentTypeDelegatingMessageConverter) {
			headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), amqpMessageProperties);
			amqpMessage = converter.toMessage(requestMessage.getPayload(), amqpMessageProperties);
		}
		else { // See INT-3002 - map headers last if we're not using a CTDMC
			amqpMessage = converter.toMessage(requestMessage.getPayload(), amqpMessageProperties);
			headerMapper.fromHeadersToRequest(requestMessage.getHeaders(), amqpMessageProperties);
		}
		checkDeliveryMode(requestMessage, amqpMessageProperties, defaultDeliveryMode);
		return amqpMessage;
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
