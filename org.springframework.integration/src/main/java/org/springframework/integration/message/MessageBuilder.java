/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public final class MessageBuilder<T> {

	private final T payload;

	private final Map<String, Object> headers = new HashMap<String, Object>();


	/**
	 * Create a new {@link Message} instance with no header values using
	 * the provided payload instance.
	 */
	private MessageBuilder(T payload) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
	}


	/**
	 * Create a builder for a new {@link Message} instance pre-populated with
	 * all of the headers copied from the provided message. The payload will
	 * also be taken from the provided message.
	 * 
	 * @param messageToCopy the Message from which all headers should be copied
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		MessageBuilder<T> builder = new MessageBuilder<T>(message.getPayload());
		builder.headers.putAll(message.getHeaders());
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with no header
	 * values using the provided payload instance.
	 * 
	 * @param payload the payload for the new message
	 */
	public static <T> MessageBuilder<T> fromPayload(T payload) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload);
		return builder;
	}


	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		this.headers.put(headerName, headerValue);
		return this;
	}

	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		if (this.headers.get(headerName) == null) {
			this.headers.put(headerName, headerValue);
		}
		return this;
	}

	public MessageBuilder<T> copyHeadersFromMessage(Message<?> message) {
		return this.copyHeaders(message.getHeaders());
	}

	public MessageBuilder<T> copyHeaders(MessageHeaders headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			if (key.equals(MessageHeaders.TIMESTAMP)) {
				continue;
			}
			this.setHeader(key, headersToCopy.get(key));
		}
		return this;
	}

	public MessageBuilder<T> copyHeadersFromMessageIfAbsent(Message<?> message) {
		return this.copyHeadersIfAbsent(message.getHeaders());
	}

	public MessageBuilder<T> copyHeadersIfAbsent(MessageHeaders headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			if (key.equals(MessageHeaders.TIMESTAMP)) {
				continue;
			}
			if (this.headers.get(key) == null) {
				this.setHeaderIfAbsent(key, headersToCopy.get(key));
			}
		}
		return this;
	}

	public MessageBuilder<T> setExpirationDate(Date expirationDate) {
		return this.setHeader(MessageHeaders.EXPIRATION_DATE, expirationDate);
	}

	public MessageBuilder<T> setCorrelationId(Object correlationId) {
		return this.setHeader(MessageHeaders.CORRELATION_ID, correlationId);
	}

	public MessageBuilder<T> setReturnAddress(Object returnAddress) {
		return this.setHeader(MessageHeaders.RETURN_ADDRESS, returnAddress);
	}

	public MessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		return this.setHeader(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
	}

	public MessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		return this.setHeader(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
	}

	public MessageBuilder<T> setPriority(MessagePriority priority) {
		return this.setHeader(MessageHeaders.PRIORITY, priority);
	}

	public Message<T> build() {
		return new GenericMessage<T>(this.payload, this.headers);
	}

}
