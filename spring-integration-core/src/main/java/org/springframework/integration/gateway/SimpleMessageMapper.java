/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gateway;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;

/**
 * An implementation of the {@link InboundMessageMapper} and
 * {@link OutboundMessageMapper} strategy interfaces that maps directly to and
 * from the Message payload instance.
 * 
 * @author Mark Fisher
 */
public class SimpleMessageMapper implements InboundMessageMapper<Object>, OutboundMessageMapper<Object> {

	/**
	 * Returns the Message payload (or null if the Message is null).
	 */
	public Object fromMessage(Message<?> message) {
		if (message == null) {
			return null;
		}
		return message.getPayload();
	}

	/**
	 * Returns a Message with the given object as its payload, unless the
	 * object is already a Message in which case it will be returned as-is.
	 * If the object is null, the returned Message will also be null.
	 */
	public Message<?> toMessage(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof Message<?>) {
			return (Message<?>) object;
		}
		return MessageBuilder.withPayload(object).build();
	}

}
