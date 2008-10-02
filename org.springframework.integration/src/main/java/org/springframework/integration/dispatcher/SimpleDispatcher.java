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

package org.springframework.integration.dispatcher;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Basic implementation of {@link MessageDispatcher} that will attempt
 * to send a {@link Message} to one of its consumers. As soon as <em>one</em>
 * of the consumers accepts the Message, the dispatcher will return 'true'.
 * <p>
 * If the dispatcher has no consumers, a {@link MessageDeliveryException}
 * will be thrown. If all consumers reject the Message, the dispatcher will
 * throw a MessageRejectedException.
 * 
 * @author Mark Fisher
 */
public class SimpleDispatcher extends AbstractDispatcher {

	public boolean dispatch(Message<?> message) {
		if (this.consumers.size() == 0) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		int count = 0;
		int rejectedExceptionCount = 0;
		for (MessageConsumer consumer : this.consumers) {
			count++;
			if (this.sendMessageToConsumer(message, consumer)) {
				return true;
			}
			rejectedExceptionCount++;
		}
		if (rejectedExceptionCount == count) {
			throw new MessageRejectedException(message, "All of dispatcher's subscribers rejected Message.");
		}
		return false;
	}

}
