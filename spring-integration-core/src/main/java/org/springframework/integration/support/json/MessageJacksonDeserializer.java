/*
 * Copyright 2017-2022 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A Jackson {@link StdNodeBasedDeserializer} extension for {@link Message} implementations.
 *
 * @param <T> the message type.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public abstract class MessageJacksonDeserializer<T extends Message<?>> extends StdNodeBasedDeserializer<T> {

	private static final long serialVersionUID = 1L;

	private JavaType payloadType = TypeFactory.defaultInstance().constructType(Object.class);

	private ObjectMapper mapper = new ObjectMapper();

	protected MessageJacksonDeserializer(Class<T> targetType) {
		super(targetType);
	}

	public void setMapper(ObjectMapper mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected final void setPayloadType(JavaType payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType = payloadType;
	}

	protected ObjectMapper getMapper() {
		return this.mapper;
	}

	@Override
	public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer td)
			throws IOException {

		return super.deserialize(jp, ctxt);
	}

	@Override
	public T convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		Map<String, Object> headers = this.mapper.readValue(root.get("headers").traverse(),
				TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class));
		Object payload = this.mapper.readValue(root.get("payload").traverse(), this.payloadType);
		return buildMessage(new MutableMessageHeaders(headers), payload, root, ctxt);
	}

	protected abstract T buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws IOException;

}
