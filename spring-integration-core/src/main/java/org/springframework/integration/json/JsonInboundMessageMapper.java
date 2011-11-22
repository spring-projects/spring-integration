/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.json;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * {@link InboundMessageMapper} implementation that maps incoming JSON messages to a {@link Message} with the specified payload type.  
 * 
 * @author Jeremy Grelle
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class JsonInboundMessageMapper implements InboundMessageMapper<String> {

	private static final String MESSAGE_FORMAT_ERROR = "JSON message is invalid.  Expected a message in the format of either " +
			"{\"headers\":{...},\"payload\":{...}} or {\"payload\":{...}.\"headers\":{...}} but was ";

	private static final Map<String, Class<?>> DEFAULT_HEADER_TYPES = new HashMap<String, Class<?>>();

	static {
		DEFAULT_HEADER_TYPES.put(MessageHeaders.PRIORITY, Integer.class);
		DEFAULT_HEADER_TYPES.put(MessageHeaders.EXPIRATION_DATE, Long.class);
		DEFAULT_HEADER_TYPES.put(MessageHeaders.SEQUENCE_SIZE, Integer.class);
		DEFAULT_HEADER_TYPES.put(MessageHeaders.SEQUENCE_NUMBER, Integer.class);
	}


	private final JavaType payloadType;

	private final Map<String, Class<?>> headerTypes = DEFAULT_HEADER_TYPES;

	private volatile ObjectMapper objectMapper = new ObjectMapper();

	private volatile boolean mapToPayload = false;


	public JsonInboundMessageMapper(Class<?> payloadType) {
		Assert.notNull(payloadType, "payloadType must not be null");
		this.payloadType = TypeFactory.defaultInstance().constructType(payloadType);
	}

	public JsonInboundMessageMapper(TypeReference<?> typeReference) {
		Assert.notNull(typeReference, "typeReference must not be null");
		this.payloadType = TypeFactory.defaultInstance().constructType(typeReference);
	}


	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public void setHeaderTypes(Map<String, Class<?>> headerTypes) {
		this.headerTypes.putAll(headerTypes);
	}

	public void setMapToPayload(boolean mapToPayload) {
		this.mapToPayload = mapToPayload;
	}

	public Message<?> toMessage(String jsonMessage) throws Exception {
		JsonParser parser = new JsonFactory().createJsonParser(jsonMessage);
		if (this.mapToPayload) {
			try {
				return MessageBuilder.withPayload(readPayload(parser, jsonMessage)).build();
			}
			catch (JsonMappingException ex) {
				throw new IllegalArgumentException("Mapping of JSON message " + jsonMessage +
						" directly to payload of type " + this.payloadType.getRawClass().getName() + " failed.", ex);
			}
		}
		else {
			String error = MESSAGE_FORMAT_ERROR + jsonMessage;
			Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
			Map<String, Object> headers = null;
			Object payload = null;
			while(parser.nextToken() != JsonToken.END_OBJECT) {
				Assert.isTrue(parser.getCurrentToken() == JsonToken.FIELD_NAME, error);
				boolean isHeadersToken = "headers".equals(parser.getCurrentName());
				boolean isPayloadToken = "payload".equals(parser.getCurrentName()); 
				Assert.isTrue(isHeadersToken || isPayloadToken, error);
				if (isHeadersToken) {
					Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
					headers = readHeaders(parser, jsonMessage);
				}
				else if (isPayloadToken) {
					parser.nextToken();
					try {
						payload = readPayload(parser, jsonMessage);
					}
					catch (JsonMappingException ex) {
						throw new IllegalArgumentException("Mapping payload of JSON message " + jsonMessage +
								" to payload type " + this.payloadType.getRawClass().getName() + " failed.", ex);
					}
				}
			}
			Assert.notNull(headers, error);
			return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		}
	}

	protected Map<String, Object> readHeaders(JsonParser parser, String jsonMessage) throws Exception{
		Map<String, Object> headers = new LinkedHashMap<String, Object>();
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String headerName = parser.getCurrentName();
			parser.nextToken();
			Class<?> headerType = this.headerTypes.containsKey(headerName) ?
					this.headerTypes.get(headerName) : Object.class;
			try {
				headers.put(headerName, this.objectMapper.readValue(parser, headerType));
			}
			catch (JsonMappingException ex) {
				throw new IllegalArgumentException("Mapping header \"" + headerName + "\" of JSON message " +
						jsonMessage + " to header type " + this.payloadType.getRawClass().getName() + " failed.", ex);
			}
		}
		return headers;
	}

	protected Object readPayload(JsonParser parser, String jsonMessage) throws Exception {
		return this.objectMapper.readValue(parser, this.payloadType);
	}

}
