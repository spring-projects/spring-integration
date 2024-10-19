/*
 * Copyright 2017-2024 the original author or authors.
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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;

import org.springframework.integration.handler.DelayHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A Jackson {@link StdNodeBasedDeserializer} extension for {@link Message} implementations.
 *
 * @author Youbin Wu
 * @since 6.4
 */
public class DelayedMessageWrapperJacksonDeserializer extends StdNodeBasedDeserializer<DelayHandler.DelayedMessageWrapper> {

	private static final long serialVersionUID = 1L;

	private ObjectMapper mapper = new ObjectMapper();

	protected DelayedMessageWrapperJacksonDeserializer() {
		super(DelayHandler.DelayedMessageWrapper.class);
	}

	public void setMapper(ObjectMapper mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	public ObjectMapper getMapper() {
		return this.mapper;
	}

	@Override
	public DelayHandler.DelayedMessageWrapper convert(JsonNode root, DeserializationContext ctxt)
			throws IOException {
		Long requestDate = this.mapper.readValue(root.get("requestDate").traverse(), Long.class);
		Message<?> message = this.mapper.readValue(root.get("original").traverse(), Message.class);
		return new DelayHandler.DelayedMessageWrapper(message, requestDate);
	}

}
