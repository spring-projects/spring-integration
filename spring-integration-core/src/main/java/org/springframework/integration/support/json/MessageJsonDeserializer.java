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

import java.util.HashMap;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdNodeBasedDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.TypeFactory;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A Jackson {@link StdNodeBasedDeserializer} extension for {@link Message} implementations.
 *
 * @param <T> the message type.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public abstract class MessageJsonDeserializer<T extends Message<?>> extends StdNodeBasedDeserializer<T> {

	private JavaType payloadType = TypeFactory.createDefaultInstance().constructType(Object.class);

	private JsonMapper mapper = new JsonMapper();

	protected MessageJsonDeserializer(Class<T> targetType) {
		super(targetType);
	}

	public void setMapper(JsonMapper mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected final void setPayloadType(JavaType payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType = payloadType;
	}

	protected JsonMapper getMapper() {
		return this.mapper;
	}

	@Override
	public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer td)
			throws JacksonException {

		return super.deserialize(jp, ctxt);
	}

	@Override
	public T convert(JsonNode root, DeserializationContext ctxt) throws JacksonException {
		Map<String, Object> headers = this.mapper.readValue(root.get("headers").traverse(ctxt),
				this.mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
		Object payload = this.mapper.readValue(root.get("payload").traverse(ctxt), this.payloadType);
		return buildMessage(new MutableMessageHeaders(headers), payload, root, ctxt);
	}

	protected abstract T buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws JacksonException;

}
