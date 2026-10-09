/*
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
 * @deprecated Since 7.0 in favor of {@link MessageHeadersJsonSerializer} for Jackson 3.
 */
@Deprecated(since = "7.0", forRemoval = true)
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
