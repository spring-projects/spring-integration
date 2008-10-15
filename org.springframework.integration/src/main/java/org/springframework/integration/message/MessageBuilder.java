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

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public final class MessageBuilder<T> {

	private final T payload;

	private final Map<String, Object> headers = new HashMap<String, Object>();

	private final Message<T> originalMessage;

	private volatile boolean modified;


	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private MessageBuilder(T payload, Message<T> originalMessage) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.originalMessage = originalMessage;
		if (originalMessage != null) {
			this.headers.putAll(originalMessage.getHeaders());
		}
	}


	/**
	 * Create a builder for a new {@link Message} instance pre-populated with
	 * all of the headers copied from the provided message. The payload of the
	 * provided Message will also be used as the payload for the new message.
	 * 
	 * @param messageToCopy the Message from which the payload and all headers
	 * will be copied
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		MessageBuilder<T> builder = new MessageBuilder<T>(message.getPayload(), message);
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 * 
	 * @param payload the payload for the new message
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload, null);
		return builder;
	}


	/**
	 * Set the value for the given header name. If the provided value is
	 * <code>null</code>, the header will be removed.
	 */
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		if (StringUtils.hasLength(headerName) && !(this.isReadOnly(headerName))) {
			this.modified = true;
			if (headerValue == null) {
				this.headers.remove(headerName);
			}
			else {
				this.headers.put(headerName, headerValue);
			}
		}
		return this;
	}

	/**
	 * Set the value for the given header name only if the header name
	 * is not already associated with a value.
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		if (this.headers.get(headerName) == null) {
			this.setHeader(headerName, headerValue);
		}
		return this;
	}

	/**
	 * Remove the value for the given header name.
	 */
	public MessageBuilder<T> removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName)) {
			this.modified = true;
			this.headers.remove(headerName);
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will
	 * overwrite any existing values. Use {{@link #copyHeadersIfAbsent(Map)}
	 * to avoid overwriting values. Note that the 'id' and 'timestamp' header
	 * values will never be overwritten.
	 * 
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	public MessageBuilder<T> copyHeaders(Map<String, Object> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			this.setHeader(key, headersToCopy.get(key));
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will
	 * <em>not</em> overwrite any existing values.
	 */
	public MessageBuilder<T> copyHeadersIfAbsent(Map<String, Object> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			this.setHeaderIfAbsent(key, headersToCopy.get(key));
		}
		return this;
	}

	public MessageBuilder<T> setExpirationDate(Long expirationDate) {
		return this.setHeader(MessageHeaders.EXPIRATION_DATE, expirationDate);
	}

	public MessageBuilder<T> setExpirationDate(Date expirationDate) {
		if (expirationDate != null) {
			return this.setHeader(MessageHeaders.EXPIRATION_DATE, expirationDate.getTime());
		}
		else {
			return this.setHeader(MessageHeaders.EXPIRATION_DATE, null);
		}
	}

	public MessageBuilder<T> setCorrelationId(Object correlationId) {
		return this.setHeader(MessageHeaders.CORRELATION_ID, correlationId);
	}

	public MessageBuilder<T> setReturnAddress(MessageChannel returnAddress) {
		return this.setHeader(MessageHeaders.RETURN_ADDRESS, returnAddress);
	}

	public MessageBuilder<T> setReturnAddress(String returnAddress) {
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
		if (!this.modified && this.originalMessage != null) {
			return this.originalMessage;
		}
		return new GenericMessage<T>(this.payload, this.headers);
	}

	private boolean isReadOnly(String key) {
		return (key.equals(MessageHeaders.ID) || key.equals(MessageHeaders.TIMESTAMP));
	}

}
