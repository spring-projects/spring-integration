/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.util.MimeType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Simple {@link StdSerializer} extension to represent a {@link MimeType} object in the
 * target JSON as a plain string.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class MimeTypeSerializer extends StdSerializer<MimeType> {

	private static final long serialVersionUID = 1L;

	public MimeTypeSerializer() {
		super(MimeType.class);
	}

	@Override
	public void serializeWithType(MimeType value, JsonGenerator generator, SerializerProvider serializers,
			TypeSerializer typeSer) throws IOException {

		serialize(value, generator, serializers);
	}

	@Override
	public void serialize(MimeType value, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeString(value.toString());
	}

}
