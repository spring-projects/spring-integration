/*
 * Copyright 2024 the original author or authors.
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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * The {@link AbstractIntegrationMessageBuilder} extension for the default logic to build message.
 * The {@link MessageBuilder} is fully based on this class.
 * This abstract class can be used for creating custom {@link Message} instances.
 * For that purpose its {@link #build()} method has to be overridden.
 * The custom {@link Message} type could be used, for example, to hide sensitive information
 * from payload and headers when message is logged.
 * For this goal there would be enough to override {@link GenericMessage#toString()}
 * and filter out (or mask) those headers which container such sensitive information.
 *
 * @param <T> the payload type.
 * @param <B> the target builder class type.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see MessageBuilder
 * @see MessageBuilderFactory
 */
public abstract class BaseMessageBuilder<T, B extends BaseMessageBuilder<T, B>>
		extends AbstractIntegrationMessageBuilder<T> {

	private static final Log LOGGER = LogFactory.getLog(BaseMessageBuilder.class);

	private final T payload;

	private final IntegrationMessageHeaderAccessor headerAccessor;

	@Nullable
	private final Message<T> originalMessage;

	private volatile boolean modified;

	private String[] readOnlyHeaders;

	protected BaseMessageBuilder(T payload, @Nullable Message<T> originalMessage) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.originalMessage = originalMessage;
		this.headerAccessor = new IntegrationMessageHeaderAccessor(originalMessage);
		if (originalMessage != null) {
			this.modified = (!this.payload.equals(originalMessage.getPayload()));
		}
	}

	@Override
	public T getPayload() {
		return this.payload;
	}

	@Override
	public Map<String, Object> getHeaders() {
		return this.headerAccessor.toMap();
	}

	@Nullable
	@Override
	public <V> V getHeader(String key, Class<V> type) {
		return this.headerAccessor.getHeader(key, type);
	}

	/**
	 * Set the value for the given header name. If the provided value is {@code null}, the header will be removed.
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this MessageBuilder.
	 */
	@Override
	public B setHeader(String headerName, @Nullable Object headerValue) {
		this.headerAccessor.setHeader(headerName, headerValue);
		return _this();
	}

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this MessageBuilder.
	 */
	@Override
	public B setHeaderIfAbsent(String headerName, Object headerValue) {
		this.headerAccessor.setHeaderIfAbsent(headerName, headerValue);
		return _this();
	}

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests the array
	 * may contain simple matching patterns for header names. Supported pattern styles are:
	 * {@code xxx*}, {@code *xxx}, {@code *xxx*} and {@code xxx*yyy}.
	 * @param headerPatterns The header patterns.
	 * @return this MessageBuilder.
	 */
	@Override
	public B removeHeaders(String... headerPatterns) {
		this.headerAccessor.removeHeaders(headerPatterns);
		return _this();
	}

	/**
	 * Remove the value for the given header name.
	 * @param headerName The header name.
	 * @return this MessageBuilder.
	 */
	@Override
	public B removeHeader(String headerName) {
		if (!this.headerAccessor.isReadOnly(headerName)) {
			this.headerAccessor.removeHeader(headerName);
		}
		else if (LOGGER.isInfoEnabled()) {
			LOGGER.info("The header [" + headerName + "] is ignored for removal because it is is readOnly.");
		}
		return _this();
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any existing values. Use
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values. Note that the 'id' and 'timestamp' header values
	 * will never be overwritten.
	 * @param headersToCopy The headers to copy.
	 * @return this MessageBuilder.
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	@Override
	public B copyHeaders(@Nullable Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeaders(headersToCopy);
		return _this();
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will not override any existing values.
	 * @param headersToCopy The headers to copy.
	 * @return this MessageBuilder.
	 */
	@Override
	public B copyHeadersIfAbsent(@Nullable Map<String, ?> headersToCopy) {
		if (headersToCopy != null) {
			for (Map.Entry<String, ?> entry : headersToCopy.entrySet()) {
				String headerName = entry.getKey();
				if (!this.headerAccessor.isReadOnly(headerName)) {
					this.headerAccessor.setHeaderIfAbsent(headerName, entry.getValue());
				}
			}
		}
		return _this();
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	protected List<List<Object>> getSequenceDetails() {
		return (List<List<Object>>) this.headerAccessor.getHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
	}

	@Override
	@Nullable
	protected Object getCorrelationId() {
		return this.headerAccessor.getCorrelationId();
	}

	@Override
	protected Object getSequenceNumber() {
		return this.headerAccessor.getSequenceNumber();
	}

	@Override
	protected Object getSequenceSize() {
		return this.headerAccessor.getSequenceSize();
	}

	@Override
	public B pushSequenceDetails(Object correlationId, int sequenceNumber, int sequenceSize) {
		super.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		return _this();
	}

	@Override
	public B popSequenceDetails() {
		super.popSequenceDetails();
		return _this();
	}

	@Override
	public B setExpirationDate(@Nullable Long expirationDate) {
		super.setExpirationDate(expirationDate);
		return _this();
	}

	@Override
	public B setExpirationDate(@Nullable Date expirationDate) {
		super.setExpirationDate(expirationDate);
		return _this();
	}

	@Override
	public B setCorrelationId(Object correlationId) {
		super.setCorrelationId(correlationId);
		return _this();
	}

	@Override
	public B setReplyChannel(MessageChannel replyChannel) {
		super.setReplyChannel(replyChannel);
		return _this();
	}

	@Override
	public B setReplyChannelName(String replyChannelName) {
		super.setReplyChannelName(replyChannelName);
		return _this();
	}

	@Override
	public B setErrorChannel(MessageChannel errorChannel) {
		super.setErrorChannel(errorChannel);
		return _this();
	}

	@Override
	public B setErrorChannelName(String errorChannelName) {
		super.setErrorChannelName(errorChannelName);
		return _this();
	}

	@Override
	public B setSequenceNumber(Integer sequenceNumber) {
		super.setSequenceNumber(sequenceNumber);
		return _this();
	}

	@Override
	public B setSequenceSize(Integer sequenceSize) {
		super.setSequenceSize(sequenceSize);
		return _this();
	}

	@Override
	public B setPriority(Integer priority) {
		super.setPriority(priority);
		return _this();
	}

	/**
	 * Specify a list of headers which should be considered as read only
	 * and prohibited from being populated in the message.
	 * @param readOnlyHeaders the list of headers for {@code readOnly} mode.
	 * Defaults to {@link MessageHeaders#ID} and {@link MessageHeaders#TIMESTAMP}.
	 * @return the current {@link BaseMessageBuilder}
	 * @see IntegrationMessageHeaderAccessor#isReadOnly(String)
	 */
	public B readOnlyHeaders(@Nullable String... readOnlyHeaders) {
		this.readOnlyHeaders = readOnlyHeaders != null ? Arrays.copyOf(readOnlyHeaders, readOnlyHeaders.length) : null;
		if (readOnlyHeaders != null) {
			this.headerAccessor.setReadOnlyHeaders(readOnlyHeaders);
		}
		return _this();
	}

	/**
	 * Return an original message instance if it is not modified and does not have read-only headers.
	 * If payload is an instance of {@link Throwable}, then an {@link ErrorMessage} is built.
	 * Otherwise, a new instance of {@link GenericMessage} is produced.
	 * This method can be overridden to provide any custom message implementations.
	 * @return the message instance
	 * @see #getPayload()
	 * @see #getHeaders()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Message<T> build() {
		if (!this.modified && !this.headerAccessor.isModified() && this.originalMessage != null
				&& !containsReadOnly(this.originalMessage.getHeaders())) {

			return this.originalMessage;
		}
		if (this.payload instanceof Throwable throwable) {
			return (Message<T>) new ErrorMessage(throwable, this.headerAccessor.toMap());
		}
		return new GenericMessage<>(this.payload, this.headerAccessor.toMap());
	}

	private boolean containsReadOnly(MessageHeaders headers) {
		if (!ObjectUtils.isEmpty(this.readOnlyHeaders)) {
			for (String readOnly : this.readOnlyHeaders) {
				if (headers.containsKey(readOnly)) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private B _this() {
		return (B) this;
	}

}
