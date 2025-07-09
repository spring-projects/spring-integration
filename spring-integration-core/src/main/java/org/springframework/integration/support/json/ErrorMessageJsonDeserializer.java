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
