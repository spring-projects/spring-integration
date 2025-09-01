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

package org.springframework.integration.support.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * For outbound messages, uses a message-aware Jackson JSON mapper to render the message
 * as JSON. For messages with {@code byte[]} payloads, if rendered as JSON, Jackson
 * performs Base64 conversion on the bytes. If payload is {@code byte[]} and
 * the {@link #setRawBytes(boolean) rawBytes} property is true (default), the result has the form
 * {@code [headersLen][headers][payloadLen][payload]}; with the headers
 * rendered in JSON and the payload unchanged.
 * Otherwise, message is fully serialized and deserialized with Jackson JSON mapper.
 * <p>
 * By default, all headers are included; you can provide simple patterns to specify a
 * subset of headers.
 * <p>
 * If neither expected format is detected, nor an error occurs during conversion, the
 * payload of the message is the original {@code byte[]}.
 * <p>
 * <b>IMPORTANT</b>
 * <p>
 * The default JSON mapper will only deserialize classes in certain packages.
 *
 * <pre class=code>
 *	"java.util",
 *	"java.lang",
 *	"org.springframework.messaging.support",
 *	"org.springframework.integration.support",
 *	"org.springframework.integration.message",
 *	"org.springframework.integration.store",
 *	"org.springframework.integration.history",
 * 	"org.springframework.integration.handler"
 * </pre>
 * <p>
 * To add more packages, create an JSON mapper using
 * {@link JacksonMessagingUtils#messagingAwareMapper(String...)}.
 * <p>
 * A constructor is provided allowing the provision of such a configured JSON mapper.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class EmbeddedHeadersJsonMessageMapper implements BytesMessageMapper {

	protected final Log logger = LogFactory.getLog(getClass());

	private final JsonMapper jsonMapper;

	private final String[] headerPatterns;

	private final boolean allHeaders;

	private boolean rawBytes = true;

	private boolean caseSensitive;

	/**
	 * Construct an instance that embeds all headers, using the default
	 * JSON Object mapper.
	 */
	public EmbeddedHeadersJsonMessageMapper() {
		this("*");
	}

	/**
	 * Construct an instance that embeds headers matching the supplied patterns, using
	 * the default JSON object mapper.
	 * @param headerPatterns the patterns.
	 * @see PatternMatchUtils#smartMatch(String, String...)
	 */
	public EmbeddedHeadersJsonMessageMapper(String... headerPatterns) {
		this(JacksonMessagingUtils.messagingAwareMapper(), headerPatterns);
	}

	/**
	 * Construct an instance that embeds all headers, using the
	 * supplied JSON object mapper.
	 * @param jsonMapper the JSON mapper.
	 */
	public EmbeddedHeadersJsonMessageMapper(JsonMapper jsonMapper) {
		this(jsonMapper, "*");
	}

	/**
	 * Construct an instance that embeds headers matching the supplied patterns using the
	 * supplied JSON object mapper.
	 * @param jsonMapper the JSON mapper.
	 * @param headerPatterns the patterns.
	 */
	public EmbeddedHeadersJsonMessageMapper(JsonMapper jsonMapper, String... headerPatterns) {
		this.jsonMapper = jsonMapper;
		this.headerPatterns = Arrays.copyOf(headerPatterns, headerPatterns.length);
		this.allHeaders = this.headerPatterns.length == 1 && this.headerPatterns[0].equals("*");
	}

	/**
	 * For messages with {@code byte[]} payloads, if rendered as JSON, Jackson performs
	 * Base64 conversion on the bytes. If this property is true (default), the result has
	 * the form {@code [headersLen][headers][payloadLen][payload]}; with
	 * the headers rendered in JSON and the payload unchanged.
	 * Set to {@code false} to render the bytes as base64.
	 * @param rawBytes false to encode as base64.
	 */
	public void setRawBytes(boolean rawBytes) {
		this.rawBytes = rawBytes;
	}

	/**
	 * Set to true to make the header name pattern match case-sensitive.
	 * Default false.
	 * @param caseSensitive true to make case-sensitive.
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public Collection<String> getHeaderPatterns() {
		return Arrays.asList(this.headerPatterns);
	}

	@Override
	public byte[] fromMessage(Message<?> message) {
		Map<String, Object> headersToEncode =
				this.allHeaders
						? message.getHeaders()
						: pruneHeaders(message.getHeaders());

		if (this.rawBytes && message.getPayload() instanceof byte[] bytes) {
			return fromBytesPayload(bytes, headersToEncode);
		}
		else {
			Message<?> messageToEncode = message;

			if (!this.allHeaders) {
				if (!headersToEncode.containsKey(MessageHeaders.ID)) {
					headersToEncode.put(MessageHeaders.ID, MessageHeaders.ID_VALUE_NONE);
				}
				if (!headersToEncode.containsKey(MessageHeaders.TIMESTAMP)) {
					headersToEncode.put(MessageHeaders.TIMESTAMP, -1L);
				}

				messageToEncode = new MutableMessage<>(message.getPayload(), headersToEncode);
			}

			try {
				return this.jsonMapper.writeValueAsBytes(messageToEncode);
			}
			catch (JacksonException ex) {
				throw new UncheckedIOException(new IOException(ex));
			}
		}
	}

	private Map<String, Object> pruneHeaders(MessageHeaders messageHeaders) {
		return messageHeaders
				.entrySet()
				.stream()
				.filter(e -> matchHeader(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean matchHeader(String header) {
		return Boolean.TRUE.equals(this.caseSensitive
				? PatternMatchUtils.smartMatch(header, this.headerPatterns)
				: PatternMatchUtils.smartMatchIgnoreCase(header, this.headerPatterns));
	}

	private byte[] fromBytesPayload(byte[] payload, Map<String, Object> headersToEncode) {
		try {
			byte[] headers = this.jsonMapper.writeValueAsBytes(headersToEncode);
			ByteBuffer buffer = ByteBuffer.wrap(new byte[8 + headers.length + payload.length]);
			buffer.putInt(headers.length);
			buffer.put(headers);
			buffer.putInt(payload.length);
			buffer.put(payload);
			return buffer.array();
		}
		catch (JacksonException ex) {
			throw new UncheckedIOException(new IOException(ex));
		}
	}

	@Override
	public Message<?> toMessage(byte[] bytes, @Nullable Map<String, Object> headers) {
		Message<?> message = null;
		try {
			message = decodeNativeFormat(bytes, headers);
		}
		catch (@SuppressWarnings("unused") Exception ex) {
			this.logger.debug("Failed to decode native format", ex);
		}
		if (message == null) {
			try {
				message = (Message<?>) this.jsonMapper.readValue(bytes, Object.class);
			}
			catch (Exception ex) {
				this.logger.debug("Failed to decode JSON", ex);
			}
		}
		if (message != null) {
			return message;
		}
		else {
			return headers == null ? new GenericMessage<>(bytes) : new GenericMessage<>(bytes, headers);
		}
	}

	@Nullable
	private Message<?> decodeNativeFormat(byte[] bytes, @Nullable Map<String, Object> headersToAdd) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		if (buffer.remaining() > 4) {
			int headersLen = buffer.getInt();
			if (headersLen >= 0 && headersLen < buffer.remaining() - 4) {
				((Buffer) buffer).position(headersLen + 4);
				int payloadLen = buffer.getInt();
				if (payloadLen != buffer.remaining()) {
					return null;
				}
				else {
					((Buffer) buffer).position(4);
					@SuppressWarnings("unchecked")
					Map<String, Object> headers = this.jsonMapper.readValue(bytes, buffer.position(), headersLen,
							Map.class);

					((Buffer) buffer).position(buffer.position() + headersLen);
					buffer.getInt();
					Object payload;
					byte[] payloadBytes = new byte[payloadLen];
					buffer.get(payloadBytes);
					payload = payloadBytes;

					if (headersToAdd != null) {
						headersToAdd.forEach(headers::putIfAbsent);
					}

					return new GenericMessage<>(payload, new MutableMessageHeaders(headers));
				}
			}
		}
		return null;
	}

}
