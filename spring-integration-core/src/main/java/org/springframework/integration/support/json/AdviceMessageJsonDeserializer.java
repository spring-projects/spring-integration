/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
