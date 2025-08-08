/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;

/**
 * The {@link MessageJacksonDeserializer} implementation for the {@link MutableMessage}.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public class MutableMessageJacksonDeserializer extends MessageJacksonDeserializer<MutableMessage<?>> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public MutableMessageJacksonDeserializer() {
		super((Class<MutableMessage<?>>) (Class<?>) MutableMessage.class);
	}

	@Override
	protected MutableMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws IOException {
		return new MutableMessage<Object>(payload, headers);
	}

}
