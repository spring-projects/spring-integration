/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.support.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.support.GenericMessage;

/**
 * The {@link MessageJsonDeserializer} implementation for the {@link GenericMessage}.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class GenericMessageJsonDeserializer extends MessageJsonDeserializer<GenericMessage<?>> {

	@SuppressWarnings("unchecked")
	public GenericMessageJsonDeserializer() {
		super((Class<GenericMessage<?>>) (Class<?>) GenericMessage.class);
	}

	@Override
	protected GenericMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws JacksonException {

		return new GenericMessage<>(payload, headers);
	}

}
