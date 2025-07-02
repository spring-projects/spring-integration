/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.codec;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * A {@link MessageConverter} that delegates to a {@link Codec} to convert.
 *
 * @author Gary Russell
 *
 * @since 4.2
 *
 */
public class CodecMessageConverter extends IntegrationObjectSupport implements MessageConverter {

	private final Codec codec;

	private final Class<?> messageClass;

	public CodecMessageConverter(Codec codec) {
		this.codec = codec;
		this.messageClass = GenericMessage.class;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		try {
			return this.codec.encode(message);
		}
		catch (IOException e) {
			throw new MessagingException(message, "Failed to encode Message", e);
		}
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		Assert.isInstanceOf(byte[].class, payload);
		try {
			Message<?> decoded = (Message<?>) this.codec.decode((byte[]) payload, this.messageClass);
			if (headers != null) {
				AbstractIntegrationMessageBuilder<?> builder = getMessageBuilderFactory().fromMessage(decoded);
				builder.copyHeaders(headers);
				return builder.build();
			}
			else {
				return decoded;
			}
		}
		catch (IOException e) {
			throw new MessagingException("Failed to decode", e);
		}
	}

}
