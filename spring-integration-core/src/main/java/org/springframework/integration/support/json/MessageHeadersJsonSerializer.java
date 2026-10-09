/*
 * Copyright 2025-present the original author or authors.
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
