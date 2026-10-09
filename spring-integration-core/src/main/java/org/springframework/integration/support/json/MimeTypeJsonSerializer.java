/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.support.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import org.springframework.util.MimeType;

/**
 * Simple {@link StdSerializer} extension to represent a {@link MimeType} object in the
 * target JSON as a plain string.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class MimeTypeJsonSerializer extends StdSerializer<MimeType> {

	public MimeTypeJsonSerializer() {
		super(MimeType.class);
	}

	@Override
	public void serializeWithType(MimeType value, JsonGenerator gen, SerializationContext ctxt,
			TypeSerializer typeSer) throws JacksonException {

		serialize(value, gen, ctxt);
	}

	@Override
	public void serialize(MimeType value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
		gen.writeString(value.toString());
	}

}
