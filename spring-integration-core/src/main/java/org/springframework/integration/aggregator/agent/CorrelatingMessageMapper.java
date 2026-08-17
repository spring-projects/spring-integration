/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator.agent;

import java.io.ObjectInputFilter;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.protobuf.ByteString;

import org.springframework.integration.aggregator.agent.grpc.HeaderList;
import org.springframework.integration.aggregator.agent.grpc.HeaderMap;
import org.springframework.integration.aggregator.agent.grpc.HeaderValue;
import org.springframework.integration.aggregator.agent.grpc.MessageEnvelope;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Maps Spring messages to the deliberately small correlating-agent wire model.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
public final class CorrelatingMessageMapper {

	private CorrelatingMessageMapper() {
	}

	public static MessageEnvelope toEnvelope(Message<?> message, CorrelatingPayloadCodec payloadCodec) {
		MessageEnvelope.Builder envelope = MessageEnvelope.newBuilder()
				.setPayload(payloadCodec.encode(message.getPayload()));
		message.getHeaders().forEach((name, value) -> {
			if (!(value instanceof MessageChannel)) {
				try {
					envelope.putHeaders(name, encodeHeader(value));
				}
				catch (IllegalArgumentException ex) {
					// The dependency gateway overlays local-only headers from the original message.
				}
			}
		});
		return envelope.build();
	}

	public static Message<?> fromEnvelope(MessageEnvelope envelope, CorrelatingPayloadCodec payloadCodec,
			ObjectInputFilter filter, Map<String, Object> localHeaders) {

		Object payload = payloadCodec.decode(envelope.getPayload(), filter);
		if (payload == null) {
			throw new IllegalArgumentException("Spring messages cannot contain a null payload");
		}
		Map<String, Object> headers = new LinkedHashMap<>();
		envelope.getHeadersMap().forEach((name, value) -> headers.put(name, decodeHeader(value)));
		headers.putAll(localHeaders);
		return new GenericMessage<>(payload, new TransportMessageHeaders(headers));
	}

	private static HeaderValue encodeHeader(Object value) {
		HeaderValue.Builder header = HeaderValue.newBuilder().setJavaType(value.getClass().getName());
		if (value instanceof String string) {
			return header.setStringValue(string).build();
		}
		if (value instanceof Character character) {
			return header.setStringValue(character.toString()).build();
		}
		if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
			return header.setIntValue(((Number) value).intValue()).build();
		}
		if (value instanceof Long longValue) {
			return header.setLongValue(longValue).build();
		}
		if (value instanceof Float || value instanceof Double) {
			return header.setDoubleValue(((Number) value).doubleValue()).build();
		}
		if (value instanceof Boolean booleanValue) {
			return header.setBooleanValue(booleanValue).build();
		}
		if (value instanceof byte[] bytes) {
			return header.setBytesValue(ByteString.copyFrom(bytes)).build();
		}
		if (value instanceof UUID uuid) {
			return header.setUuidValue(uuid.toString()).build();
		}
		if (value instanceof Date date) {
			return header.setLongValue(date.getTime()).build();
		}
		if (value instanceof Instant instant) {
			return header.setStringValue(instant.toString()).build();
		}
		if (value instanceof URI || value instanceof MimeType) {
			return header.setStringValue(value.toString()).build();
		}
		if (value instanceof Collection<?> collection) {
			HeaderList.Builder list = HeaderList.newBuilder();
			collection.forEach(element -> list.addValues(encodeHeader(element)));
			return header.setListValue(list).build();
		}
		if (value instanceof Object[] array) {
			HeaderList.Builder list = HeaderList.newBuilder();
			for (Object element : array) {
				list.addValues(encodeHeader(element));
			}
			return header.setListValue(list).build();
		}
		if (value instanceof Map<?, ?> map) {
			HeaderMap.Builder headerMap = HeaderMap.newBuilder();
			map.forEach((key, element) -> {
				if (!(key instanceof String stringKey)) {
					throw new IllegalArgumentException("Correlating agent header maps require String keys");
				}
				headerMap.putValues(stringKey, encodeHeader(element));
			});
			return header.setMapValue(headerMap).build();
		}
		throw new IllegalArgumentException("Unsupported correlating agent header type: " + value.getClass().getName());
	}

	private static Object decodeHeader(HeaderValue header) {
		return switch (header.getValueCase()) {
			case STRING_VALUE -> decodeString(header.getStringValue(), header.getJavaType());
			case INT_VALUE -> decodeInteger(header.getIntValue(), header.getJavaType());
			case LONG_VALUE -> decodeLong(header.getLongValue(), header.getJavaType());
			case BOOLEAN_VALUE -> header.getBooleanValue();
			case DOUBLE_VALUE -> Float.class.getName().equals(header.getJavaType())
					? (float) header.getDoubleValue() : header.getDoubleValue();
			case BYTES_VALUE -> header.getBytesValue().toByteArray();
			case UUID_VALUE -> UUID.fromString(header.getUuidValue());
			case LIST_VALUE -> decodeList(header.getListValue());
			case MAP_VALUE -> decodeMap(header.getMapValue());
			case VALUE_NOT_SET -> throw new IllegalArgumentException("Correlating agent header has no value");
		};
	}

	private static Object decodeString(String value, String javaType) {
		if (Character.class.getName().equals(javaType)) {
			return value.charAt(0);
		}
		if (Instant.class.getName().equals(javaType)) {
			return Instant.parse(value);
		}
		if (URI.class.getName().equals(javaType)) {
			return URI.create(value);
		}
		if (MimeType.class.getName().equals(javaType)) {
			return MimeTypeUtils.parseMimeType(value);
		}
		return value;
	}

	private static Object decodeInteger(int value, String javaType) {
		if (Byte.class.getName().equals(javaType)) {
			return (byte) value;
		}
		if (Short.class.getName().equals(javaType)) {
			return (short) value;
		}
		return value;
	}

	private static Object decodeLong(long value, String javaType) {
		return Date.class.getName().equals(javaType) ? new Date(value) : value;
	}

	private static List<Object> decodeList(HeaderList list) {
		List<Object> result = new ArrayList<>(list.getValuesCount());
		list.getValuesList().forEach(value -> result.add(decodeHeader(value)));
		return result;
	}

	private static Map<String, Object> decodeMap(HeaderMap map) {
		Map<String, Object> result = new LinkedHashMap<>();
		map.getValuesMap().forEach((key, value) -> result.put(key, decodeHeader(value)));
		return result;
	}

	private static final class TransportMessageHeaders extends MessageHeaders {

		private static final long serialVersionUID = 1L;

		TransportMessageHeaders(Map<String, Object> headers) {
			super(headers, headers.get(ID) instanceof UUID id ? id : null,
					headers.get(TIMESTAMP) instanceof Long timestamp ? timestamp : null);
		}

	}

}
