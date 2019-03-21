/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.integration.support.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * The simple {@link MessageConverter} implementation which contract is to return
 * {@link Message} as is for both {@code from/to} operations.
 * <p>
 * It is useful in cases of some protocol implementations (e.g. STOMP),
 * which is based on the "Spring Messaging Foundation" and the further logic
 * operates only with {@link Message}s, e.g. Spring Integration Adapters.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public class PassThruMessageConverter implements MessageConverter {

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		return message;
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		Assert.isInstanceOf(byte[].class, payload, "'payload' must be of 'byte[]' type.");
		return MessageBuilder.createMessage(payload, headers);
	}

}
