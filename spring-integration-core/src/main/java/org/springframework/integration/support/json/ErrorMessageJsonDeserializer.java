/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.support.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.type.TypeFactory;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * The {@link MessageJsonDeserializer} implementation for the {@link ErrorMessage}.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class ErrorMessageJsonDeserializer extends MessageJsonDeserializer<ErrorMessage> {

	@SuppressWarnings("this-escape")
	public ErrorMessageJsonDeserializer() {
		super(ErrorMessage.class);
		setPayloadType(TypeFactory.createDefaultInstance().constructType(Throwable.class));
	}

	@Override
	protected ErrorMessage buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws JacksonException {

		Message<?> originalMessage = getMapper().readValue(root.get("originalMessage").traverse(ctxt), Message.class);
		return new ErrorMessage((Throwable) payload, headers, originalMessage);
	}

}
