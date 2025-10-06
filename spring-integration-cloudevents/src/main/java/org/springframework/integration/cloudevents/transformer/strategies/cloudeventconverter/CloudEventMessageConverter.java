/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents.transformer.strategies.cloudeventconverter;

import java.nio.charset.StandardCharsets;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.impl.GenericStructuredMessageReader;
import io.cloudevents.core.message.impl.MessageUtils;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * A {@link MessageConverter} that can translate to and from a {@link Message
 * Message&lt;byte[]>} or {@link Message Message&lt;String>} and a {@link CloudEvent}.
 *
 * @author Dave Syer
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class CloudEventMessageConverter implements MessageConverter {

	public static final String CE_PREFIX = "ce-";

	public static final String SPEC_VERSION = CE_PREFIX + "specversion";

	public static final String CONTENT_TYPE = CE_PREFIX + "datacontenttype";

	private final String cePrefix;

	public CloudEventMessageConverter(String cePrefix) {
		this.cePrefix = cePrefix;
	}

	public CloudEventMessageConverter() {
		this(CE_PREFIX);
	}

	/**
	 Convert the payload of a Message from a CloudEvent to a typed Object of the specified target class.
	 If the converter does not support the specified media type or cannot perform the conversion, it should return null.
	 * @param message the input message
	 * @param targetClass This method does not check the class since it is expected to be a {@link CloudEvent}
	 * @return the result of the conversion, or null if the converter cannot perform the conversion
	 */
	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		return createMessageReader(message, this.cePrefix).toEvent();
	}

	@Override
	public Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		Assert.state(payload instanceof CloudEvent, "Payload must be a CloudEvent");
		Assert.state(headers != null, "Headers must not be null");
		return CloudEventUtils.toReader((CloudEvent) payload).read(new MessageBuilderMessageWriter(headers, this.cePrefix));
	}

	private static MessageReader createMessageReader(Message<?> message, String cePrefix) {
		return MessageUtils.parseStructuredOrBinaryMessage(
				() -> contentType(message.getHeaders()),
				format -> structuredMessageReader(message, format),
				() -> version(message.getHeaders()),
				version -> binaryMessageReader(message, version, cePrefix)
		);
	}

	private static @Nullable String version(MessageHeaders message) {
		if (message.containsKey(SPEC_VERSION)) {
			return message.get(SPEC_VERSION).toString();
		}
		return null;
	}

	private static MessageReader binaryMessageReader(Message<?> message, SpecVersion version, String cePrefix) {
		return new MessageBinaryMessageReader(version, message.getHeaders(), getBinaryData(message), cePrefix);
	}

	private static MessageReader structuredMessageReader(Message<?> message, EventFormat format) {
		return new GenericStructuredMessageReader(format, getBinaryData(message));
	}

	private static @Nullable String contentType(MessageHeaders message) {
		if (message.containsKey(MessageHeaders.CONTENT_TYPE)) {
			return message.get(MessageHeaders.CONTENT_TYPE).toString();
		}
		if (message.containsKey(CONTENT_TYPE)) {
			return message.get(CONTENT_TYPE).toString();
		}
		return null;
	}

	private static byte[] getBinaryData(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof byte[] bytePayload) {
			return bytePayload;
		}
		else if (payload instanceof String stringPayload) {
			return stringPayload.getBytes(StandardCharsets.UTF_8);
		}
		throw new IllegalStateException("Message payload must be a byte array or a String");
	}

}
