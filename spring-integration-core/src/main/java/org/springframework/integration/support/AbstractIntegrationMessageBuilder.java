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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public abstract class AbstractIntegrationMessageBuilder<T> {

	/**
	 * Set the value for the given header name. If the provided value is <code>null</code>, the header will be removed.
	 *
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> setHeader(String headerName, Object headerValue);

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 *
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue);

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests the array
	 * may contain simple matching patterns for header names. Supported pattern styles are:
	 * "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 *
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
	 *
	 * @param headersToCopy The headers to copy.
	 * @return this.
	 *
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	public abstract AbstractIntegrationMessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy);

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em> overwrite any existing values.
	 *
	 * @param headersToCopy The headers to copy.
	 * @return this.
	 */
	public abstract AbstractIntegrationMessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy);

	public AbstractIntegrationMessageBuilder<T> setExpirationDate(Long expirationDate) {
		return this.setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, expirationDate);
	}

	public AbstractIntegrationMessageBuilder<T> setExpirationDate(Date expirationDate) {
		if (expirationDate != null) {
			return this.setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, expirationDate.getTime());
		}
		else {
			return this.setHeader(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, null);
		}
	}

	public AbstractIntegrationMessageBuilder<T> setCorrelationId(Object correlationId) {
		return this.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
	}

	public AbstractIntegrationMessageBuilder<T> pushSequenceDetails(Object correlationId, int sequenceNumber,
			int sequenceSize) {
		Object incomingCorrelationId = this.getCorrelationId();
		List<List<Object>> incomingSequenceDetails = this.getSequenceDetails();
		if (incomingCorrelationId != null) {
			if (incomingSequenceDetails == null) {
				incomingSequenceDetails = new ArrayList<List<Object>>();
			}
			else {
				incomingSequenceDetails = new ArrayList<List<Object>>(incomingSequenceDetails);
			}
			incomingSequenceDetails.add(Arrays.asList(incomingCorrelationId,
					this.getSequenceNumber(), this.getSequenceSize()));
			incomingSequenceDetails = Collections.unmodifiableList(incomingSequenceDetails);
		}
		if (incomingSequenceDetails != null) {
			this.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		return setCorrelationId(correlationId).setSequenceNumber(sequenceNumber).setSequenceSize(sequenceSize);
	}

	public AbstractIntegrationMessageBuilder<T> popSequenceDetails() {
		List<List<Object>> incomingSequenceDetails = this.getSequenceDetails();
		if (incomingSequenceDetails == null) {
			return this;
		}
		else {
			incomingSequenceDetails = new ArrayList<List<Object>>(incomingSequenceDetails);
		}
		List<Object> sequenceDetails = incomingSequenceDetails.remove(incomingSequenceDetails.size() - 1);
		Assert.state(sequenceDetails.size() == 3, "Wrong sequence details (not created by MessageBuilder?): "
				+ sequenceDetails);
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
			this.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		else {
			this.removeHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		}
		return this;
	}

	protected abstract List<List<Object>> getSequenceDetails();

	protected abstract Object getCorrelationId();

	protected abstract Object getSequenceNumber();

	protected abstract Object getSequenceSize();

	public AbstractIntegrationMessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		return this.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public AbstractIntegrationMessageBuilder<T> setReplyChannelName(String replyChannelName) {
		return this.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public AbstractIntegrationMessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		return this.setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

	public AbstractIntegrationMessageBuilder<T> setErrorChannelName(String errorChannelName) {
		return this.setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public AbstractIntegrationMessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		return this.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
	}

	public AbstractIntegrationMessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		return this.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
	}

	public AbstractIntegrationMessageBuilder<T> setPriority(Integer priority) {
		return this.setHeader(IntegrationMessageHeaderAccessor.PRIORITY, priority);
	}

	public abstract Message<T> build();

}
