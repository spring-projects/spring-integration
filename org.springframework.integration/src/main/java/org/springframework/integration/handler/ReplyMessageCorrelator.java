/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractMessageHandlingEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;

/**
 * A handler for receiving messages from a "reply channel".
 * 
 * @author Mark Fisher
 */
public class ReplyMessageCorrelator extends AbstractMessageHandlingEndpoint {

	@Override
	public Message<?> handle(Message<?> message) {
		Object returnAddress = message.getHeaders().getReturnAddress();
		if (returnAddress == null) {
			throw new MessageHandlingException(message,
					"unable to correlate response, message has no returnAddress");
		}
		MessageChannel replyChannel = null;
		if (returnAddress instanceof MessageChannel) {
			replyChannel = (MessageChannel) returnAddress;
		}
		else if (returnAddress instanceof String) {
			replyChannel = this.getChannelRegistry().lookupChannel((String) returnAddress);
			if (replyChannel == null) {
				throw new MessagingException(message,
						"unable to resolve returnAddress '" + returnAddress + "'");
			}
		}
		else {
			throw new MessagingException(message,
					"invalid returnAddress type [" + returnAddress.getClass() + "]");
		}
		if (replyChannel != null) {
			replyChannel.send(message);
		}
		return null;
	}

}
