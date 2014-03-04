/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The default message builder; creates immutable {@link GenericMessage}s.
 * Named MessageBuilder instead of DefaultMessageBuilder for backwards
 * compatibility.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 */
public final class MessageBuilder<T> extends AbstractIntegrationMessageBuilder<T> {

	private final T payload;

	private final IntegrationMessageHeaderAccessor headerAccessor;

	private final Message<T> originalMessage;

	private volatile boolean modified;

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private MessageBuilder(T payload, Message<T> originalMessage) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.originalMessage = originalMessage;
		this.headerAccessor = new IntegrationMessageHeaderAccessor(originalMessage);
		if (originalMessage != null) {
			this.modified = (!this.payload.equals(originalMessage.getPayload()));
		}
	}

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all of the headers copied from the
	 * provided message. The payload of the provided Message will also be used as the payload for the new message.
	 *
	 * @param message the Message from which the payload and all headers will be copied
	 * @param <T> The type of the payload.
	 * @return A MessageBuilder.
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
	 * @param <T> The type of the payload.
	 * @return A MessageBuilder.
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload, null);
		return builder;
	}

	/**
	 * Set the value for the given header name. If the provided value is <code>null</code>, the header will be removed.
	 *
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this MessageBuilder.
	 */
	@Override
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		this.headerAccessor.setHeader(headerName,  headerValue);
		return this;
	}

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 *
	 * @param headerName The header name.
	 * @param headerValue The header value.
	 * @return this MessageBuilder.
	 */
	@Override
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		this.headerAccessor.setHeaderIfAbsent(headerName, headerValue);
		return this;
	}

	/**
	 * Removes all headers provided via array of 'headerPatterns'. As the name suggests the array
	 * may contain simple matching patterns for header names. Supported pattern styles are:
	 * "xxx*", "*xxx", "*xxx*" and "xxx*yyy".
	 *
	 * @param headerPatterns The header patterns.
	 * @return this MessageBuilder.
	 */
	@Override
	public MessageBuilder<T> removeHeaders(String... headerPatterns) {
		this.headerAccessor.removeHeaders(headerPatterns);
		return this;
	}
	/**
	 * Remove the value for the given header name.
	 * @param headerName The header name.
	 * @return this MessageBuilder.
	 */
	@Override
	public MessageBuilder<T> removeHeader(String headerName) {
		this.headerAccessor.removeHeader(headerName);
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any existing values. Use {
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values. Note that the 'id' and 'timestamp' header values
	 * will never be overwritten.
	 *
	 * @param headersToCopy The headers to copy.
	 * @return this MessageBuilder.
	 *
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	@Override
	public MessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeaders(headersToCopy);
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em> overwrite any existing values.
	 *
	 * @param headersToCopy The headers to copy.
	 * @return this MessageBuilder.
	 */
	@Override
	public MessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		this.headerAccessor.copyHeadersIfAbsent(headersToCopy);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<List<Object>> getSequenceDetails() {
		return (List<List<Object>>) this.headerAccessor.getHeader(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
	}

	@Override
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

	/*
	 * The following overrides (delegating to super) are provided to ease the
	 *  pain for existing applications that use the builder API and expect
	 *  a MessageBuilder to be returned.
	 */
	@Override
	public MessageBuilder<T> pushSequenceDetails(Object correlationId, int sequenceNumber, int sequenceSize) {
		super.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		return this;
	}

	@Override
	public MessageBuilder<T> popSequenceDetails() {
		super.popSequenceDetails();
		return this;
	}

	@Override
	public MessageBuilder<T> setExpirationDate(Long expirationDate) {
		super.setExpirationDate(expirationDate);
		return this;
	}

	@Override
	public MessageBuilder<T> setExpirationDate(Date expirationDate) {
		super.setExpirationDate(expirationDate);
		return this;
	}

	@Override
	public MessageBuilder<T> setCorrelationId(Object correlationId) {
		super.setCorrelationId(correlationId);
		return this;
	}

	@Override
	public MessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		super.setReplyChannel(replyChannel);
		return this;
	}

	@Override
	public MessageBuilder<T> setReplyChannelName(String replyChannelName) {
		super.setReplyChannelName(replyChannelName);
		return this;
	}

	@Override
	public MessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		super.setErrorChannel(errorChannel);
		return this;
	}

	@Override
	public MessageBuilder<T> setErrorChannelName(String errorChannelName) {
		super.setErrorChannelName(errorChannelName);
		return this;
	}

	@Override
	public MessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		super.setSequenceNumber(sequenceNumber);
		return this;
	}

	@Override
	public MessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		super.setSequenceSize(sequenceSize);
		return this;
	}

	@Override
	public MessageBuilder<T> setPriority(Integer priority) {
		super.setPriority(priority);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Message<T> build() {
		if (!this.modified && !this.headerAccessor.isModified() && this.originalMessage != null) {
			return this.originalMessage;
		}
		if (this.payload instanceof Throwable) {
			return (Message<T>) new ErrorMessage((Throwable) this.payload, this.headerAccessor.toMap());
		}
		return new GenericMessage<T>(this.payload, this.headerAccessor.toMap());
	}

}
