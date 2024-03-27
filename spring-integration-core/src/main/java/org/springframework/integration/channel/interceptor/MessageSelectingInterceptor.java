/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.channel.interceptor;

import java.util.Arrays;
import java.util.List;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * A {@link org.springframework.messaging.support.ChannelInterceptor ChannelInterceptor} that
 * delegates to a list of {@link MessageSelector MessageSelectors} to decide
 * whether a {@link Message} should be accepted on the {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MessageSelectingInterceptor implements ChannelInterceptor {

	private final List<MessageSelector> selectors;

	public MessageSelectingInterceptor(MessageSelector... selectors) {
		this.selectors = Arrays.asList(selectors);
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		for (MessageSelector selector : this.selectors) {
			if (!selector.accept(message)) {
				throw new MessageDeliveryException(message,
						"selector '" + selector + "' did not accept message");
			}
		}
		return message;
	}

}
