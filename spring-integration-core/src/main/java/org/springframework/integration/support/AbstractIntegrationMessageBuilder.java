/*
 * Copyright 2014-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public abstract class AbstractIntegrationMessageBuilder<T> {

	public AbstractIntegrationMessageBuilder<T> setExpirationDate(Long expirationDate) {
		return setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, expirationDate);
	}

	public AbstractIntegrationMessageBuilder<T> setExpirationDate(@Nullable Date expirationDate) {
		if (expirationDate != null) {
			return setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, expirationDate.getTime());
		}
		else {
			return setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, null);
		}
	}

	public AbstractIntegrationMessageBuilder<T> setCorrelationId(Object correlationId) {
		return setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
	}

	public AbstractIntegrationMessageBuilder<T> pushSequenceDetails(Object correlationId, int sequenceNumber,
			int sequenceSize) {

		Object incomingCorrelationId = getCorrelationId();
		List<List<Object>> incomingSequenceDetails = getSequenceDetails();
		if (incomingCorrelationId != null) {
			if (incomingSequenceDetails == null) {
				incomingSequenceDetails = new ArrayList<>();
			}
			else {
				incomingSequenceDetails = new ArrayList<>(incomingSequenceDetails);
			}
			incomingSequenceDetails.add(Arrays.asList(incomingCorrelationId,
					getSequenceNumber(), getSequenceSize()));
			incomingSequenceDetails = Collections.unmodifiableList(incomingSequenceDetails);
		}
		if (incomingSequenceDetails != null) {
			setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		return setCorrelationId(correlationId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize);
	}

	public AbstractIntegrationMessageBuilder<T> popSequenceDetails() {
		List<List<Object>> incomingSequenceDetails = getSequenceDetails();
		if (incomingSequenceDetails == null) {
			return this;
		}
		else {
			incomingSequenceDetails = new ArrayList<>(incomingSequenceDetails);
		}
		List<Object> sequenceDetails = incomingSequenceDetails.remove(incomingSequenceDetails.size() - 1);
		Assert.state(sequenceDetails.size() == 3, // NOSONAR
				() -> "Wrong sequence details (not created by MessageBuilder?): " + sequenceDetails);
		setCorrelationId(sequenceDetails.get(0));
		Integer sequenceNumber = (Integer) sequenceDetails.get(1);
		Integer sequenceSize = (Integer) sequenceDetails.get(2);
		if (sequenceNumber != null) {
			setSequenceNumber(sequenceNumber);
		}
		if (sequenceSize != null) {
			setSequenceSize(sequenceSize);
		}
		if (!incomingSequenceDetails.isEmpty()) {
			setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		else {
			removeHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		}
		return this;
	}

	public AbstractIntegrationMessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		return setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public AbstractIntegrationMessageBuilder<T> setReplyChannelName(String replyChannelName) {
		return setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public AbstractIntegrationMessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		return setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

	public AbstractIntegrationMessageBuilder<T> setErrorChannelName(String errorChannelName) {
		return setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public AbstractIntegrationMessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		return setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
	}

	public AbstractIntegrationMessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		return setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
	}

	public AbstractIntegrationMessageBuilder<T> setPriority(Integer priority) {
		return setHeader(IntegrationMessageHeaderAccessor.PRIORITY, priority);
	}

	/**
	 * Remove headers from the provided map matching to the provided pattens
	 * and only after that copy the result into the target message headers.
	 * @param headersToCopy a map of headers to copy.
	 * @param headerPatternsToFilter an arrays of header patterns to filter before copying.
	 * @return the current {@link AbstractIntegrationMessageBuilder}.
	 * @since 5.1
	 * @see #copyHeadersIfAbsent(Map)
	 */
	public AbstractIntegrationMessageBuilder<T> filterAndCopyHeadersIfAbsent(Map<String, ?> headersToCopy,
			@Nullable String... headerPatternsToFilter) {

		Map<String, ?> headers = headersToCopy;

		if (!ObjectUtils.isEmpty(headerPatternsToFilter)) {
			headers = new HashMap<>(headersToCopy);
			headers.entrySet()
					.removeIf(entry -> PatternMatchUtils.simpleMatch(headerPatternsToFilter, entry.getKey()));
		}

		return copyHeadersIfAbsent(headers);
	}

	@Nullable
	protected abstract List<List<Object>> getSequenceDetails();

	@Nullable
	protected abstract Object getCorrelationId();

	@Nullable
	protected abstract Object getSequenceNumber();

	@Nullable
	protected abstract Object getSequenceSize();

	public abstract T getPayload();

	public abstract Map<String, Object> getHeaders();

	@Nullable
	public abstract <V> V getHeader(String key, Class<V> type);

	/**
	 * Set the value for the given header name. If the provided value is <code>null</code>, the header will be removed.
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> setHeader(String headerName, @Nullable Object headerValue);

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue);

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests the array
	 * may contain simple matching patterns for header names. Supported pattern styles are:
	 * "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 * @param headerPatterns The header patterns.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> removeHeaders(String... headerPatterns);

	/**
	 * Remove the value for the given header name.
	 * @param headerName The header name.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> removeHeader(String headerName);

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any existing values. Use {
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values. Note that the 'id' and 'timestamp' header values
	 * will never be overwritten.
	 * @param headersToCopy The headers to copy.
	 * @return this.
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	public abstract AbstractIntegrationMessageBuilder<T> copyHeaders(@Nullable Map<String, ?> headersToCopy);

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em> overwrite any existing values.
	 * @param headersToCopy The headers to copy.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy);

	public abstract Message<T> build();

}
