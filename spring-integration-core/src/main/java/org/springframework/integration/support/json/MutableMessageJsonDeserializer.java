/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;

import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;

/**
 * The {@link MessageJsonDeserializer} implementation for the {@link MutableMessage}.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class MutableMessageJsonDeserializer extends MessageJsonDeserializer<MutableMessage<?>> {

	@SuppressWarnings("unchecked")
	public MutableMessageJsonDeserializer() {
		super((Class<MutableMessage<?>>) (Class<?>) MutableMessage.class);
	}

	@Override
	protected MutableMessage<?> buildMessage(MutableMessageHeaders headers, Object payload, JsonNode root,
			DeserializationContext ctxt) throws JacksonException {

		return new MutableMessage<>(payload, headers);
	}

}
