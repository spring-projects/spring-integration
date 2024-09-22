/*
 * Copyright 2017-2024 the original author or authors.
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
 * @author Ngoc Nhan
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
		return new AdviceMessage<>(payload, (MessageHeaders) headers, inputMessage);
	}

}
