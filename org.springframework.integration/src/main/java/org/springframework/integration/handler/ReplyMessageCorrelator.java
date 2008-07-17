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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.RetrievalBlockingMessageStore;
import org.springframework.util.Assert;

/**
 * A handler for receiving messages from a "reply channel". Any component that
 * is expecting a reply message can poll by providing the correlation identifier.
 * 
 * @author Mark Fisher
 */
public class ReplyMessageCorrelator implements MessageHandler {

	private volatile long defaultTimeout = 5000;

	private final RetrievalBlockingMessageStore messageStore;


	public ReplyMessageCorrelator(int capacity) {
		this.messageStore = new RetrievalBlockingMessageStore(capacity);
	}


	public void setDefaultTimeout(long defaultTimeout) {
		Assert.isTrue(defaultTimeout >= 0, "'defaultTimeout' must not be negative");
		this.defaultTimeout = defaultTimeout;
	}

	public Message<?> handle(Message<?> message) {
		Object correlationId = this.getCorrelationId(message);
		if (correlationId == null) {
			throw new MessageHandlingException(message, 
					"unable to handle response, message has no correlationId: " + message);
		}
		this.messageStore.put(correlationId, message);
		return null;
	}

	public Message<?> getReply(Object correlationId) {
		return this.getReply(correlationId, this.defaultTimeout);
	}

	public Message<?> getReply(Object correlationId, long timeout) {
		Assert.notNull(correlationId, "'correlationId' must not be null");
		return this.messageStore.remove(correlationId, timeout);
	}

	/**
	 * Retrieve the correlation identifier from the provided message.
	 * <p>
	 * This method may be overridden by subclasses. The default implementation
	 * returns the 'correlationId' from the message header.
	 */
	protected Object getCorrelationId(final Message<?> message) {
		return message.getHeaders().getCorrelationId();
	}

}
