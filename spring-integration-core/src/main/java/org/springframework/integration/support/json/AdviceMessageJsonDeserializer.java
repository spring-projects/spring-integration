/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.support.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;

/**
 * The {@link MessageJsonDeserializer} implementation for the {@link AdviceMessage}.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class AdviceMessageJsonDeserializer extends MessageJsonDeserializer<AdviceMessage<?>> {

	@SuppressWarnings("unchecked")
	public AdviceMessageJsonDeserializer() {
		super((Class<AdviceMessage<?>>) (Class<?>) AdviceMessage.class);
	}

	@Override
	protected AdviceMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws JacksonException {

		Message<?> inputMessage = getMapper().readValue(root.get("inputMessage").traverse(ctxt), Message.class);
		return new AdviceMessage<>(payload, headers, inputMessage);
	}

}
