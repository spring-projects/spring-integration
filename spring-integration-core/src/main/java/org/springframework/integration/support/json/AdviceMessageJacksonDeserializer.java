/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * The {@link MessageJacksonDeserializer} implementation for the {@link AdviceMessage}.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public class AdviceMessageJacksonDeserializer extends MessageJacksonDeserializer<AdviceMessage<?>> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public AdviceMessageJacksonDeserializer() {
		super((Class<AdviceMessage<?>>) (Class<?>) AdviceMessage.class);
	}

	@Override
	protected AdviceMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws IOException {
		Message<?> inputMessage = getMapper().readValue(root.get("inputMessage").traverse(), Message.class);
		return new AdviceMessage<Object>(payload, (MessageHeaders) headers, inputMessage);
	}

}
