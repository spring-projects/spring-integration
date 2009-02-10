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

import java.util.Map;

import org.springframework.integration.core.Message;

/**
 * Utility class for AbstractMessageBarrierHandler and its subclasses for
 * storing objects while in transit. It is a wrapper around a {@link Map},
 * providing special properties for recording the complete status, the creation
 * time (for determining if a group of messages has timed out), and the
 * correlation id for a group of messages (available after the first message has
 * been added to it). This is a parameterized type, allowing different different
 * client classes to use different types of Maps and their respective features.
 * 
 * This class is not thread-safe and will be synchronized by the calling code.
 * 
 * @author Marius Bogoevici
 */
public class MessageBarrier<T extends Map<K, Message<?>>, K> {

	protected final T messages;

	private volatile boolean complete = false;

    private Object correlationKey;

	private final long timestamp = System.currentTimeMillis();

	public MessageBarrier(T messages, Object correlationKey) {
		this.messages = messages;
		this.correlationKey = correlationKey;
	}

	public Object getCorrelationKey() {
		return this.correlationKey;
	}

	/**
	 * Returns the creation time of this barrier as the number of milliseconds
	 * since January 1, 1970.
	 * 
	 * @see System#currentTimeMillis()
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Marks the barrier as complete.
	 */
	public void setComplete() {
		this.complete = true;
	}

	/**
	 * True if the barrier has received all the messages and can proceed to
	 * release them.
	 */
	public boolean isComplete() {
		return this.complete;
	}

	public T getMessages() {
		return this.messages;
	}

}
