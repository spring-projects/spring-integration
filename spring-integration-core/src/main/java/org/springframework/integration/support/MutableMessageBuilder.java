/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Specialized message builder that can be used within a component to avoid the overhead
 * of having to build multiple messages for mutations within that component.
 *
 * <p>
 * <b>IMPORTANT: Mutable messages may share state (such as message headers); such messages
 * should never be exposed to other components or undesirable side-effects may result.</b>
 * <p>
 * <b>It is generally recommended that messages transferred between components should
 * always be immutable unless great care is taken with their use.</b>
 *
 * @param <T> the payload type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public final class MutableMessageBuilder<T> extends AbstractIntegrationMessageBuilder<T> {

	private final MutableMessage<T> mutableMessage;

	private final Map<String, Object> headers;

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private MutableMessageBuilder(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		if (message instanceof MutableMessage) {
			this.mutableMessage = (MutableMessage<T>) message;
		}
		else {
			this.mutableMessage = new MutableMessage<T>(message.getPayload(), message.getHeaders());
		}
		this.headers = this.mutableMessage.getRawHeaders();
	}

	@Override
	public T getPayload() {
		return this.mutableMessage.getPayload();
	}

	@Override
	public Map<String, Object> getHeaders() {
		return this.headers;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <V> V getHeader(String key, Class<V> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" + key + "'. Expected [" + type
					+ "] but actual type is [" + value.getClass() + "]");
		}
		return (V) value;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 * @param payload the payload for the new message
	 * @param <T> The type of the payload.
	 * @return A MutableMessageBuilder.
	 */
	public static <T> MutableMessageBuilder<T> withPayload(T payload) {
		return withPayload(payload, true);
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 * The {@code generateHeaders} flag allows to disable {@link MessageHeaders#ID}
	 * and {@link MessageHeaders#TIMESTAMP} headers generation.
	 * @param payload the payload for the new message
	 * @param generateHeaders whether generate {@link MessageHeaders#ID}
	 * and {@link MessageHeaders#TIMESTAMP} headers
	 * @param <T> The type of the payload.
	 * @return A MutableMessageBuilder.
	 * @since 5.0
	 */
	public static <T> MutableMessageBuilder<T> withPayload(T payload, boolean generateHeaders) {
		MutableMessage<T> message;
		if (generateHeaders) {
			message = new MutableMessage<>(payload);
		}
		else {
			message = new MutableMessage<>(payload, new MutableMessageHeaders(null, MessageHeaders.ID_VALUE_NONE, -1L));
		}

		return fromMessage(message);
	}

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all of the headers copied from the
	 * provided message. The payload of the provided Message will also be used as the payload for the new message.
	 * @param message the Message from which the payload and all headers will be copied
	 * @param <T> The type of the payload.
	 * @return A MutableMessageBuilder.
	 */
	public static <T> MutableMessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		return new MutableMessageBuilder<T>(message);
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> setHeader(String headerName, @Nullable Object headerValue) {
		Assert.notNull(headerName, "'headerName' must not be null");
		if (headerValue == null) {
			this.removeHeader(headerName);
		}
		else {
			this.headers.put(headerName, headerValue);
		}
		return this;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		if (!this.headers.containsKey(headerName)) {
			this.headers.put(headerName, headerValue);
		}
		return this;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> removeHeaders(String... headerPatterns) {
		List<String> headersToRemove = new ArrayList<>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)) {
				if (pattern.contains("*")) {
					headersToRemove.addAll(getMatchingHeaderNames(pattern, this.headers));
				}
				else {
					headersToRemove.add(pattern);
				}
			}
		}
		for (String headerToRemove : headersToRemove) {
			removeHeader(headerToRemove);
		}
		return this;
	}

	private List<String> getMatchingHeaderNames(String pattern, Map<String, Object> headers) {
		List<String> matchingHeaderNames = new ArrayList<>();
		if (headers != null) {
			for (Map.Entry<String, Object> header : headers.entrySet()) {
				if (PatternMatchUtils.simpleMatch(pattern, header.getKey())) {
					matchingHeaderNames.add(header.getKey());
				}
			}
		}
		return matchingHeaderNames;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName)) {
			this.headers.remove(headerName);
		}
		return this;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			this.headers.putAll(headersToCopy);
		}
		return this;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			for (Entry<String, ?> entry : headersToCopy.entrySet()) {
				setHeaderIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<List<Object>> getSequenceDetails() {
		return (List<List<Object>>) this.headers.get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
	}

	@Override
	protected Object getCorrelationId() {
		return this.headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID);
	}

	@Override
	protected Object getSequenceNumber() {
		return this.headers.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER);
	}

	@Override
	protected Object getSequenceSize() {
		return this.headers.get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE);
	}

	@Override
	public Message<T> build() {
		return this.mutableMessage;
	}

}
