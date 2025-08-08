/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * The {@link MessageJacksonDeserializer} implementation for the {@link ErrorMessage}.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public class ErrorMessageJacksonDeserializer extends MessageJacksonDeserializer<ErrorMessage> {

	private static final long serialVersionUID = 1L;

	public ErrorMessageJacksonDeserializer() {
		super(ErrorMessage.class);
		setPayloadType(TypeFactory.defaultInstance().constructType(Throwable.class));
	}

	@Override
	protected ErrorMessage buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws IOException {
		Message<?> originalMessage = getMapper().readValue(root.get("originalMessage").traverse(), Message.class);
		return new ErrorMessage((Throwable) payload, headers, originalMessage);
	}

}
