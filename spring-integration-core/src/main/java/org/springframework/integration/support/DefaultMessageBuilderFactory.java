/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.support;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.0
 *
 */
public class DefaultMessageBuilderFactory implements MessageBuilderFactory {

	private String @Nullable [] readOnlyHeaders;

	/**
	 * Specify a list of headers which should be considered as a read only
	 * and prohibited from the population to the message.
	 * @param readOnlyHeaders the list of headers for {@code readOnly} mode.
	 * Defaults to {@link org.springframework.messaging.MessageHeaders#ID}
	 * and {@link org.springframework.messaging.MessageHeaders#TIMESTAMP}.
	 * @since 4.3.2
	 */
	public void setReadOnlyHeaders(String @Nullable ... readOnlyHeaders) {
		this.readOnlyHeaders = readOnlyHeaders != null ? Arrays.copyOf(readOnlyHeaders, readOnlyHeaders.length) : null;
	}

	/**
	 * Add headers to the configured list of read only headers.
	 * @param readOnlyHeaders the additional headers.
	 * @since 4.3.10
	 */
	public void addReadOnlyHeaders(String... readOnlyHeaders) {
		String[] headers = this.readOnlyHeaders;
		if (headers == null || headers.length == 0) {
			headers = Arrays.copyOf(readOnlyHeaders, readOnlyHeaders.length);
		}
		else {
			headers = Arrays.copyOf(headers, headers.length + readOnlyHeaders.length);
			System.arraycopy(readOnlyHeaders, 0, headers, (this.readOnlyHeaders != null) ? this.readOnlyHeaders.length : 0, readOnlyHeaders.length);
		}
		this.readOnlyHeaders = headers;
	}

	@Override
	public <T> MessageBuilder<T> fromMessage(Message<T> message) {
		return MessageBuilder.fromMessage(message)
				.readOnlyHeaders(this.readOnlyHeaders);
	}

	@Override
	public <T> MessageBuilder<T> withPayload(T payload) {
		return MessageBuilder.withPayload(payload)
				.readOnlyHeaders(this.readOnlyHeaders);
	}

}
