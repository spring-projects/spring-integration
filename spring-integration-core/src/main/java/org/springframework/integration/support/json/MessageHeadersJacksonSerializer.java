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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.springframework.messaging.MessageHeaders;

/**
 * A Jackson {@link StdSerializer} implementation to serialize {@link MessageHeaders}
 * as a {@link HashMap}.
 * <p>
 * This technique is much reliable during deserialization, especially when the
 * {@code typeId} property is used to store the type.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public class MessageHeadersJacksonSerializer extends StdSerializer<MessageHeaders> {

	private static final long serialVersionUID = 1L;

	public MessageHeadersJacksonSerializer() {
		super(MessageHeaders.class);
	}

	@Override
	public void serializeWithType(MessageHeaders value, JsonGenerator gen, SerializerProvider serializers,
			TypeSerializer typeSer) throws IOException {
		serialize(value, gen, serializers);
	}

	@Override
	public void serialize(MessageHeaders value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeObject(new HashMap<String, Object>(value));
	}

}
