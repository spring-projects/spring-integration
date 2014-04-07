/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;


/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MutableMessageBuilder<T> extends AbstractIntegrationMessageBuilder<T> {

	private MutableMessage<T> mutableMessage;

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

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all of the headers copied from the
	 * provided message. The payload of the provided Message will also be used as the payload for the new message.
	 *
	 * @param message the Message from which the payload and all headers will be copied
	 * @param <T> The type of the payload.
	 * @return A MutableMessageBuilder.
	 */
	public static <T> MutableMessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		MutableMessageBuilder<T> builder = new MutableMessageBuilder<T>(message);
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 *
	 * @param payload the payload for the new message
	 * @param <T> The type of the payload.
	 * @return A MessageBuilder.
	 */
	public static <T> MutableMessageBuilder<T> withPayload(T payload) {
		MutableMessageBuilder<T> builder = new MutableMessageBuilder<T>(new MutableMessage<T>(payload));
		return builder;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> setHeader(String headerName, Object headerValue) {
		Assert.notNull(headerName);
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
		List<String> headersToRemove = new ArrayList<String>();
		for (String pattern : headerPatterns) {
			if (StringUtils.hasLength(pattern)){
				if (pattern.contains("*")){
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
		List<String> matchingHeaderNames = new ArrayList<String>();
		if (headers != null) {
			for (Map.Entry<String, Object> header: headers.entrySet()) {
				if (PatternMatchUtils.simpleMatch(pattern,  header.getKey())) {
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
	public AbstractIntegrationMessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			this.headers.putAll(headersToCopy);
		}
		return this;
	}

	@Override
	public AbstractIntegrationMessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
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
