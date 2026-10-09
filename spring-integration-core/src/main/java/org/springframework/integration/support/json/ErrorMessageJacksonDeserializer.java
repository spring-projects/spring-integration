/*
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
 * @deprecated Since 7.0 in favor of {@link ErrorMessageJsonDeserializer} for Jackson 3.
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class ErrorMessageJacksonDeserializer extends MessageJacksonDeserializer<ErrorMessage> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("this-escape")
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
