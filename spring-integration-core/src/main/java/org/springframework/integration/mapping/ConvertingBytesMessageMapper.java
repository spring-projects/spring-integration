/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.mapping;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * The {@link BytesMessageMapper} implementation to delegate to/from {@link Message}
 * conversion into the provided {@link MessageConverter}.
 * <p>
 * The {@link MessageConverter} must not return {@code null} from its
 * {@link MessageConverter#fromMessage(Message, Class)} and {@link MessageConverter#toMessage(Object, MessageHeaders)}
 * methods.
 * <p>
 * If {@link MessageConverter#fromMessage(Message, Class)} returns {@link String}, it is converted to {@link byte[]}
 * using a {@link StandardCharsets#UTF_8} encoding.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ConvertingBytesMessageMapper implements BytesMessageMapper {

	private final MessageConverter messageConverter;

	public ConvertingBytesMessageMapper(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	@Override
	@NonNull
	public Message<?> toMessage(byte[] bytes, @Nullable Map<String, Object> headers) {
		MessageHeaders messageHeaders = null;
		if (headers != null) {
			messageHeaders = new MessageHeaders(headers);
		}
		Message<?> message = this.messageConverter.toMessage(bytes, messageHeaders);
		Assert.state(message != null, () ->
				"the '" + this.messageConverter + "' produced null for bytes:" + Arrays.toString(bytes));
		return message;
	}

	@Override
	@NonNull
	public byte[] fromMessage(Message<?> message) {
		Object result = this.messageConverter.fromMessage(message, byte[].class);
		Assert.state(result != null, () -> "the '" + this.messageConverter + "' produced null for message: " + message);
		return result instanceof String
				? ((String) result).getBytes(StandardCharsets.UTF_8)
				: (byte[]) result;
	}

}
