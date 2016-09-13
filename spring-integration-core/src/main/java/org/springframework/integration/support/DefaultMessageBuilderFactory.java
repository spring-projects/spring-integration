/*
 * Copyright 2014-2016 the original author or authors.
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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class DefaultMessageBuilderFactory implements MessageBuilderFactory {

	private String[] readOnlyHeaders;

	/**
	 * Specify a list of headers which should be considered as a read only
	 * and prohibited from the population to the message.
	 * @param readOnlyHeaders the list of headers for {@code readOnly} mode.
	 * Defaults to {@link MessageHeaders#ID} and {@link MessageHeaders#TIMESTAMP}.
	 * @since 4.3.2
	 */
	public void setReadOnlyHeaders(String... readOnlyHeaders) {
		this.readOnlyHeaders = readOnlyHeaders;
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
