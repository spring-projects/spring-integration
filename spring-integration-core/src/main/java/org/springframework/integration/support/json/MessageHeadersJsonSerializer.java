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

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import org.springframework.messaging.MessageHeaders;

/**
 * A Jackson {@link StdSerializer} implementation to serialize {@link MessageHeaders}
 * as a {@link HashMap}.
 * <p>
 * This technique is much reliable during deserialization, especially when the
 * {@code typeId} property is used to store the type.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class MessageHeadersJsonSerializer extends StdSerializer<MessageHeaders> {

	public MessageHeadersJsonSerializer() {
		super(MessageHeaders.class);
	}

	@Override
	public void serializeWithType(MessageHeaders value, JsonGenerator gen, SerializationContext ctxt,
			TypeSerializer typeSer) throws JacksonException {

		serialize(value, gen, ctxt);
	}

	@Override
	public void serialize(MessageHeaders value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
		gen.writePOJO(new HashMap<>(value));
	}

}
