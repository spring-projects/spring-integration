/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.UUID;

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
 * TODO - Need to figure out if we need to go as deep in mapping HeaderTypes...right now it wouldn't work if the header type was something like List<TestBean>
 *      - cannot assume order as implemented; headers may not always precede the payload
 * 
 * @author Jeremy Grelle
 * @since 2.0
 */
public class InboundJsonMessageMapper implements
		InboundMessageMapper<String> {

	private static final String MESSAGE_FORMAT_ERROR = "JSON message is invalid.  Expected a message in the format of {\"headers\":{...},\"payload\":{...}} but was ";
	
	private ObjectMapper objectMapper = new ObjectMapper();

	private static Map<String, Class<?>> DEFAULT_HEADER_TYPES;
	
	private Map<String, Class<?>> headerTypes = DEFAULT_HEADER_TYPES;

	private boolean mapToPayload = false;
	
	private JavaType payloadType;
	
	static {
		DEFAULT_HEADER_TYPES = new HashMap<String, Class<?>>();
		DEFAULT_HEADER_TYPES.put(MessageHeaders.ID, UUID.class);
		DEFAULT_HEADER_TYPES.put(MessageHeaders.TIMESTAMP, Long.class);
		DEFAULT_HEADER_TYPES.put(MessageHeaders.EXPIRATION_DATE, Long.class);
	}
	
	public InboundJsonMessageMapper(Class<?> payloadType) {
		this.payloadType = TypeFactory.type(payloadType);
	}
	
	public InboundJsonMessageMapper(TypeReference<?> typeReference) {
		this.payloadType = TypeFactory.type(typeReference);
	}
	
	public void setHeaderTypes(Map<String, Class<?>> headerTypes) {
		this.headerTypes.putAll(headerTypes);
	}

	public void setMapToPayload(boolean mapToPayload) {
		this.mapToPayload = mapToPayload;
	}

	public Message<?> toMessage(String jsonMessage) throws Exception {
		JsonParser parser = new JsonFactory().createJsonParser(jsonMessage);
		if (mapToPayload) {
			try {
				Object payload = objectMapper.readValue(parser, payloadType);
				return MessageBuilder.withPayload(payload).build();
			} catch (JsonMappingException ex) {
				throw new IllegalArgumentException("Mapping of JSON message "+jsonMessage+" directly to payload of type "+payloadType.getRawClass().getName()+" failed.", ex);
			}
		} else {
			String error = MESSAGE_FORMAT_ERROR + jsonMessage;
			Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
			Assert.isTrue(parser.nextToken() == JsonToken.FIELD_NAME, error);
			Assert.isTrue(parser.getCurrentName().equals("headers"), error);
			Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
			Map<String, Object> headers = new LinkedHashMap<String, Object>();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String headerName = parser.getCurrentName();
				parser.nextToken();
				Class<?> headerType = headerTypes.containsKey(headerName) ? headerTypes.get(headerName) : Object.class;
				try {
					headers.put(headerName, objectMapper.readValue(parser, headerType));
				} catch (JsonMappingException ex) {
					throw new IllegalArgumentException("Mapping header \""+headerName+"\" of JSON message "+jsonMessage+" to header type "+payloadType.getRawClass().getName()+" failed.", ex);
				}
			}
			Assert.isTrue(parser.nextToken() == JsonToken.FIELD_NAME, error);
			Assert.isTrue(parser.getCurrentName().equals("payload"), error);
			parser.nextToken();
			try {
				Object payload = objectMapper.readValue(parser, payloadType);
				return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
			} catch (JsonMappingException ex) {
				throw new IllegalArgumentException("Mapping payload of JSON message "+jsonMessage+" to payload type "+payloadType.getRawClass().getName()+" failed.", ex);
			}
		}
	}
}
