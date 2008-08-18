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
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageTarget;

/**
 * Basic implementation of {@link MessageDispatcher} that will attempt
 * to send a {@link Message} to one of its targets. As soon as <em>one</em>
 * of the targets accepts the Message, the dispatcher will return 'true'.
 * <p>
 * If all targets reject the Message, the dispatcher will throw a
 * MessageRejectedException. If all targets return 'false' (e.g. due
 * to a timeout), the dispatcher will return 'false'.
 * 
 * @author Mark Fisher
 */
public class SimpleDispatcher extends AbstractDispatcher {

	public boolean send(Message<?> message) {
		if (this.targets.size() == 0) {
			if (logger.isWarnEnabled()) {
				logger.warn("Dispatcher has no targets.");
			}
			return false;
		}
		int count = 0;
		int rejectedExceptionCount = 0;
		for (MessageTarget target : this.targets) {
			count++;
			try {
				if (this.sendMessageToTarget(message, target)) {
					return true;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to send message to target, continuing with other targets if available.");
				}
			}
			catch (MessageRejectedException e) {
				rejectedExceptionCount++;
				if (logger.isDebugEnabled()) {
					logger.debug("Target '" + target + "' rejected Message, continuing with other targets if available.", e);
				}
			}
		}
		if (rejectedExceptionCount == count) {
			throw new MessageRejectedException(message, "All of dispatcher's targets rejected Message.");
		}
		return false;
	}

}
