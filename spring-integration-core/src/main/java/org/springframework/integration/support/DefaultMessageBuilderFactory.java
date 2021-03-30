/*
 * Copyright 2014-2021 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class DefaultMessageBuilderFactory implements MessageBuilderFactory {

	private String[] readOnlyHeaders;

	/**
	 * Specify a list of headers which should be considered as a read only
	 * and prohibited from the population to the message.
	 * @param readOnlyHeaders the list of headers for {@code readOnly} mode.
	 * Defaults to {@link org.springframework.messaging.MessageHeaders#ID}
	 * and {@link org.springframework.messaging.MessageHeaders#TIMESTAMP}.
	 * @since 4.3.2
	 */
	public void setReadOnlyHeaders(@Nullable String... readOnlyHeaders) {
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
			System.arraycopy(readOnlyHeaders, 0, headers, this.readOnlyHeaders.length, readOnlyHeaders.length);
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
