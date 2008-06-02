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

package org.springframework.integration.router;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * MessageBarrier implementation for message aggregation. Delegates to a
 * {@link CompletionStrategy} to determine when the group of messages is ready
 * for aggregation.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregationBarrier implements MessageBarrier {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final List<Message<?>> messages = new CopyOnWriteArrayList<Message<?>>();

	private final CompletionStrategy completionStrategy;

	private volatile boolean complete = false;

	private final ReentrantLock lock = new ReentrantLock();

	private final long timestamp = System.currentTimeMillis();


	/**
	 * Create an AggregationBarrier with the given {@link CompletionStrategy}.
	 */
	public AggregationBarrier(CompletionStrategy completionStrategy) {
		Assert.notNull(completionStrategy, "'completionStrategy' must not be null");
		this.completionStrategy = completionStrategy;
	}


	/**
	 * Returns the creation time of this barrier as the number of milliseconds
	 * since January 1, 1970.
	 * @see java.lang.System#currentTimeMillis()
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Adds a message to the aggregation group and releases <em>if complete</em>.
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
			this.messages.add(message);
			this.complete = completionStrategy.isComplete(this.messages);
			return (this.complete) ? this.messages : null;
		}
		finally {
			this.lock.unlock();
		}
	}

	public List<Message<?>> getMessages() {
		return this.messages;
	}

}
