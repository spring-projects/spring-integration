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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;

/**
 * Default implementation for a {@link MessageBarrier}.
 * 
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageBarrier implements MessageBarrier {

	private final Log logger = LogFactory.getLog(this.getClass());

	protected final List<Message<?>> messages = new ArrayList<Message<?>>();

	private volatile boolean complete = false;

	private final ReentrantLock lock = new ReentrantLock();

	private final long timestamp = System.currentTimeMillis();


	/**
	 * Returns the creation time of this barrier as the number of milliseconds
	 * since January 1, 1970.
	 * @see System#currentTimeMillis()
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	protected boolean isComplete() {
		return this.complete;
	}

	/**
	 * Adds a message to the aggregation group and releases <em>if available</em>.
	 * Otherwise, the return value will be <code>null</code>.
	 */
	public List<Message<?>> addAndRelease(Message<?> message) {
		try {
			this.lock.lock();
			if (this.complete) {
				if (logger.isDebugEnabled()) {
					logger.debug("Message received after aggregation has already completed: " + message);
				}
				return null;
			}
			this.addMessage(message);
			this.complete = this.hasReceivedAllMessages();
			return this.releaseAvailableMessages();
		}
		finally {
			this.lock.unlock();
		}
	}

	protected void addMessage(Message<?> message) {
		this.messages.add(message);
	}

	public List<Message<?>> getMessages() {
		return Collections.unmodifiableList(this.messages);
	}

	/**
	 * Subclasses must implement this method to indicate if all possible messages that could be received by
	 * a given barrier have already been received (e.g. all messages from a given sequence).
	 */
	protected abstract boolean hasReceivedAllMessages();


	/**
	 * Subclasses must implement this method to return the messages that can be released by this barrier after
	 * the receipt of a given message. It might be possible that a number of messages are released before the barrier
	 * has ended its work (partial release) and this depends completely on the implementation of the barrier.
	 * However, once hasReceivedAllMessages() is deemed true, only one call to releaseAvailableMessages() shall
	 * yield results.
	 */
	protected abstract List<Message<?>> releaseAvailableMessages();

}
