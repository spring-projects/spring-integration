/*
 * Copyright 2017-2022 the original author or authors.
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
