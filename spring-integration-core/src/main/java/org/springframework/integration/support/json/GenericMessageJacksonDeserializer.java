/*
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * The {@link MessageJacksonDeserializer} implementation for the {@link GenericMessage}.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 * @deprecated Since 7.0 in favor of {@link GenericMessageJsonDeserializer} for Jackson 3.
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class GenericMessageJacksonDeserializer extends MessageJacksonDeserializer<GenericMessage<?>> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public GenericMessageJacksonDeserializer() {
		super((Class<GenericMessage<?>>) (Class<?>) GenericMessage.class);
	}

	@Override
	protected GenericMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws IOException {
		return new GenericMessage<Object>(payload, headers);
	}

}
