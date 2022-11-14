/*
 * Copyright 2022 the original author or authors.
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

	public MessageReceiverContext(Message<?> message, @Nullable String handlerName) {
		super((carrier, key) -> carrier.getHeaders().get(key, String.class));
		this.message = message;
		this.handlerName = handlerName != null ? handlerName : "unknown";
	}

	@Override
	public Message<?> getCarrier() {
		return this.message;
	}

	public String getHandlerName() {
		return this.handlerName;
	}

}
