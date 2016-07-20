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

package org.springframework.integration.support;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link MessageBuilder} extension, which fully delegates its operations
 * to the provided {@link #delegate}.
 * A custom extension can override any {@link MessageBuilder} operation on its own.
 *
 * @author Artem Bilan
 * @since 4.2.9
 */
public class DelegatingMessageBuilder<T> extends MessageBuilder<T> {

	private final MessageBuilder<T> delegate;

	public DelegatingMessageBuilder(MessageBuilder<T> delegate) {
		super(delegate.getPayload(), null);
		this.delegate = delegate;
	}

	@Override
	public T getPayload() {
		return this.delegate.getPayload();
	}

	@Override
	public Map<String, Object> getHeaders() {
		return this.delegate.getHeaders();
	}

	@Override
	public DelegatingMessageBuilder<T> setHeader(String headerName, Object headerValue) {
		this.delegate.setHeader(headerName, headerValue);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		this.delegate.setHeaderIfAbsent(headerName, headerValue);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> removeHeaders(String... headerPatterns) {
		this.delegate.removeHeaders(headerPatterns);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> removeHeader(String headerName) {
		this.delegate.removeHeader(headerName);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		this.delegate.copyHeaders(headersToCopy);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> copyHeadersIfAbsent(Map<String, ?> headersToCopy) {
		this.delegate.copyHeadersIfAbsent(headersToCopy);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setExpirationDate(Long expirationDate) {
		this.delegate.setExpirationDate(expirationDate);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setExpirationDate(Date expirationDate) {
		this.delegate.setExpirationDate(expirationDate);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setCorrelationId(Object correlationId) {
		this.delegate.setCorrelationId(correlationId);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> pushSequenceDetails(Object correlationId, int sequenceNumber, int sequenceSize) {
		this.delegate.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> popSequenceDetails() {
		this.delegate.popSequenceDetails();
		return this;
	}

	@Override
	public List<List<Object>> getSequenceDetails() {
		return this.delegate.getSequenceDetails();
	}

	@Override
	public Object getCorrelationId() {
		return this.delegate.getCorrelationId();
	}

	@Override
	public Object getSequenceNumber() {
		return this.delegate.getSequenceNumber();
	}

	@Override
	public Object getSequenceSize() {
		return this.delegate.getSequenceSize();
	}

	@Override
	public DelegatingMessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		this.delegate.setReplyChannel(replyChannel);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setReplyChannelName(String replyChannelName) {
		this.delegate.setReplyChannelName(replyChannelName);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		this.delegate.setErrorChannel(errorChannel);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setErrorChannelName(String errorChannelName) {
		this.delegate.setErrorChannelName(errorChannelName);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		this.delegate.setSequenceNumber(sequenceNumber);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		this.delegate.setSequenceSize(sequenceSize);
		return this;
	}

	@Override
	public DelegatingMessageBuilder<T> setPriority(Integer priority) {
		this.delegate.setPriority(priority);
		return this;
	}

	@Override
	public Message<T> build() {
		return this.delegate.build();
	}

}
