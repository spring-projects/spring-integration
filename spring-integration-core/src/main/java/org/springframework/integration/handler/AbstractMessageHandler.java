/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.core.Ordered;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for MessageHandler implementations that provides basic validation
 * and error handling capabilities. Asserts that the incoming Message is not
 * null and that it does not contain a null payload. Converts checked exceptions
 * into runtime {@link MessagingException}s.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractMessageHandler extends IntegrationObjectSupport implements MessageHandler, TrackableComponent, Orderable {

	private volatile boolean shouldTrack = false;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;


	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String getComponentType() {
		return "message-handler";
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public final void handleMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(message.getPayload(), "Message payload must not be null");
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + message);
		}
		try {
			if (message != null && this.shouldTrack) {
				message = MessageHistory.write(message, this, this.getMessageBuilderFactory());
			}
			this.handleMessageInternal(message);
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessageHandlingException(message, "error occurred in message handler [" + this + "]", e);
		}
	}

	protected abstract void handleMessageInternal(Message<?> message) throws Exception;

}
