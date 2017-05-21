/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support.json;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * For outbound messages, uses a message-aware Jackson object mapper to render the message
 * as JSON. For messages with {@code byte[]} payloads, if rendered as JSON, Jackson
 * performs Base64 conversion on the bytes. If the {@link #setRawBytes(boolean) rawBytes}
 * property is true (default), the result has the form
 * &lt;headersLen&gt;&lt;headers&gt;&lt;payloadLen&gt;&lt;payload&gt;; with the headers
 * rendered in JSON and the payload unchanged.
 * <p>
 * By default, all headers are included; you can provide regular expressions to specify a
 * subset of headers.
 * <p>
 * For inbound messages, in addition to pure JSON and the above format, the Spring Cloud
 * Stream {@code EmbeddedHeadersMessageConverter} format is also supported:
 * {@code 0xff, n(1), [ [lenHdr(1), hdr, lenValue(4), value] ... ]}. The 0xff indicates
 * this format; n is the number of headers (max 255); for each header, the name length (1
 * byte) is followed by the name, followed by the value length (int) followed by the value
 * (json).
 * <p>
 * If neither special format is detected, or an error occurs during conversion, the
 * payload of the message is the original {@code byte[]}.
 * <p>
 * <b>IMPORTANT</b>
 * <p>
 * The default object mapper uses default typing. From the Jacskson Javadocs:
 * <p>
 * NOTE: use of Default Typing can be a potential security risk if incoming content comes
 * from untrusted sources, and it is recommended that this is either not done, or, if
 * enabled, use {@link ObjectMapper#setDefaultTyping} passing a custom
 * {@link com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder} implementation that
 * white-lists legal types to use.
 * <p>
 * A constructor is provided allowing the provision of such a configured object mapper.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class EmbeddedJsonHeadersMessageMapper implements BytesMessageMapper {

	private final ObjectMapper objectMapper;

	private final List<Pattern> headerPatterns;

	private final boolean allHeaders;

	private boolean rawBytes = true;

	/**
	 * Construct an instance that embeds all headers, using the default
	 * JSON Object mapper.
	 */
	public EmbeddedJsonHeadersMessageMapper() {
		this(Collections.singleton(Pattern.compile(".*")));
	}

	/**
	 * Construct an instance that embeds headers matching the supplied patterns, using
	 * the default JSON object mapper.
	 * @param headerPatterns the patterns.
	 */
	public EmbeddedJsonHeadersMessageMapper(Collection<Pattern> headerPatterns) {
		this(JacksonJsonUtils.messagingAwareMapper(), headerPatterns);
	}

	/**
	 * Construct an instance that embeds headers matching the supplied patterns using the
	 * supplied JSON object mapper.
	 * @param objectMapper the object mapper.
	 * @param headerPatterns the patterns.
	 */
	public EmbeddedJsonHeadersMessageMapper(ObjectMapper objectMapper, Collection<Pattern> headerPatterns) {
		this.objectMapper = objectMapper;
		this.headerPatterns = new ArrayList<>(headerPatterns);
		this.allHeaders = this.headerPatterns.size() == 1 && this.headerPatterns.get(0).pattern().equals(".*");
	}

	/**
	 * Set to false to encode byte[] payloads as base64 in JSON. When true,
	 * byte[] payloads are rendered as discussed in the class description.
	 * @param rawBytes false to encode as base64.
	 */
	public void setRawBytes(boolean rawBytes) {
		this.rawBytes = rawBytes;
	}

	public Collection<Pattern> getHeaderPatterns() {
		return Collections.unmodifiableCollection(this.headerPatterns);
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] fromMessage(Message<?> message) throws Exception {
		Message<?> messageToEncode = this.allHeaders ? message : pruneHeaders(message);
		if (this.rawBytes && message.getPayload() instanceof byte[]) {
			return fromBytesPayload((Message<byte[]>) messageToEncode);
		}
		else {
			return this.objectMapper.writeValueAsBytes(messageToEncode);
		}
	}

	private Message<?> pruneHeaders(Message<?> message) {
		Map<String, Object> headersToEmbed = new HashMap<>();
		message.getHeaders().forEach((k, v) -> {
			if (this.headerPatterns.stream().anyMatch(p -> p.matcher(k).matches())) {
				headersToEmbed.put(k, v);
			}
		});
		return new MutableMessage<>(message.getPayload(), headersToEmbed);
	}

	private byte[] fromBytesPayload(Message<byte[]> message) throws UnsupportedEncodingException, Exception {
		byte[] headers = this.objectMapper.writeValueAsBytes(message.getHeaders());
		byte[] payload = message.getPayload();
		ByteBuffer buffer = ByteBuffer.wrap(new byte[8 + headers.length + payload.length]);
		buffer.putInt(headers.length);
		buffer.put(headers);
		buffer.putInt(payload.length);
		buffer.put(payload);
		return buffer.array();
	}

	@Override
	public Message<?> toMessage(byte[] bytes) throws Exception {
		Message<?> message = null;
		try {
			message = decodeNativeFormat(bytes);
		}
		catch (Exception e) {
			// empty
		}
		if (message == null) {
			try {
				message = decodeSCStFormat(bytes);
			}
			catch (Exception e) {
				// empty
			}
		}
		if (message == null) {
			try {
				message = (Message<?>) this.objectMapper.readValue(bytes, Object.class);
			}
			catch (Exception e) {
				// empty
			}
		}
		if (message != null) {
			return message;
		}
		else {
			return new GenericMessage<>(bytes);
		}
	}

	private Message<?> decodeNativeFormat(byte[] bytes) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		if (buffer.remaining() > 4) {
			int headersLen = buffer.getInt();
			if (headersLen >= 0 && headersLen < buffer.remaining() - 4) {
				buffer.position(headersLen + 4);
				int payloadLen = buffer.getInt();
				if (payloadLen != buffer.remaining()) {
					buffer.position(0);
					return null;
				}
				else {
					buffer.position(4);
					@SuppressWarnings("unchecked")
					Map<String, Object> headers = this.objectMapper.readValue(bytes, buffer.position(), headersLen,
							HashMap.class);
					buffer.position(buffer.position() + headersLen);
					buffer.getInt();
					Object payload;
					byte[] payloadBytes = new byte[payloadLen];
					buffer.get(payloadBytes);
					payload = payloadBytes;
					return new GenericMessage<Object>(payload, new MutableMessageHeaders(headers));
				}
			}
		}
		buffer.position(0);
		return null;
	}

	private Message<?> decodeSCStFormat(byte[] bytes) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		int headerCount = buffer.get() & 0xff;
		if (headerCount != 255) {
			return null;
		}
		else {
			headerCount = buffer.get() & 0xff;
			Map<String, Object> headers = new HashMap<String, Object>();
			for (int i = 0; i < headerCount; i++) {
				int len = buffer.get() & 0xff;
				String headerName = new String(bytes, buffer.position(), len, "UTF-8");
				buffer.position(buffer.position() + len);
				len = buffer.getInt();
				Object headerContent = this.objectMapper.readValue(bytes, buffer.position(), len, Object.class);
				headers.put(headerName, headerContent);
				buffer.position(buffer.position() + len);
			}
			byte[] newPayload = new byte[buffer.remaining()];
			buffer.get(newPayload);
			fixIdIfPresent(headers);
			return new GenericMessage<>(newPayload, new MutableMessageHeaders(headers));
		}
	}

	private void fixIdIfPresent(Map<String, Object> headers) {
		if (headers.containsKey(MessageHeaders.ID) && headers.get(MessageHeaders.ID) instanceof String) {
			headers.put(MessageHeaders.ID, UUID.fromString((String) headers.get(MessageHeaders.ID)));
		}
	}

}
