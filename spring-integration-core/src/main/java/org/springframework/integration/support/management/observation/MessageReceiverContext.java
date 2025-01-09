/*
 * Copyright 2022-2025 the original author or authors.
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

package org.springframework.integration.support.management.observation;

import java.nio.charset.StandardCharsets;

import io.micrometer.observation.transport.ReceiverContext;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * The {@link ReceiverContext} extension for {@link Message} context.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class MessageReceiverContext extends ReceiverContext<Message<?>> {

	private final Message<?> message;

	private final String handlerName;

	private final String handlerType;

	public MessageReceiverContext(Message<?> message, @Nullable String handlerName) {
		this(message, handlerName, "handler");
	}

	/**
	 * Construct an instance based on the message, the handler (or source, producer) bean name and handler type.
	 * @param message the received message for this context.
	 * @param handlerName the handler (or source, producer) bean name processing the message.
	 * @param handlerType the handler type: {@code handler}, or {@code message-source}, or {@code message-producer}.
	 * @since 6.5
	 */
	public MessageReceiverContext(Message<?> message, @Nullable String handlerName, String handlerType) {
		super(MessageReceiverContext::getHeader);
		this.message = message;
		this.handlerName = handlerName != null ? handlerName : "unknown";
		this.handlerType = handlerType;
	}

	@Override
	public Message<?> getCarrier() {
		return this.message;
	}

	public String getHandlerName() {
		return this.handlerName;
	}

	public String getHandlerType() {
		return this.handlerType;
	}

	@Nullable
	private static String getHeader(Message<?> message, String key) {
		Object value = message.getHeaders().get(key);
		return value instanceof byte[] bytes
				? new String(bytes, StandardCharsets.UTF_8)
				: (value != null ? value.toString() : null);
	}

}
