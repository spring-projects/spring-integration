/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.springframework.util.MimeType;

/**
 * Simple {@link StdSerializer} extension to represent a {@link MimeType} object in the
 * target JSON as a plain string.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 * @deprecated Since 7.0 in favor of {@link MimeTypeJsonSerializer} for Jackson 3.
 */
@Deprecated(since = "7.0", forRemoval = true)
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
