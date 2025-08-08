/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
