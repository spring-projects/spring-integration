/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.integration.support.ComponentMetadata;

/**
 * Iterable list of {@link MessageHistoryEvent} instances.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistory implements Iterable<MessageHistoryEvent>, Serializable {

	private final List<MessageHistoryEvent> events = new ArrayList<MessageHistoryEvent>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();


	/**
	 * Add a new event with the provided component metadata.
	 */
	public MessageHistoryEvent addEvent(ComponentMetadata metadata) {
		if (metadata != null && metadata.getComponentName() != null) {
			try {
				this.lock.writeLock().lock();
				MessageHistoryEvent event = new MessageHistoryEvent(metadata);
				this.events.add(event);
				return event;
			}
			finally {
				this.lock.writeLock().unlock();
			}
		}
		return null;
	}

	/**
	 * Returns an iterator for an unmodifiable list of the history events.
	 */
	public Iterator<MessageHistoryEvent> iterator() {
		try {
			this.lock.readLock().lock();
			return Collections.unmodifiableList(this.events).iterator();
		}
		finally {
			this.lock.readLock().unlock();
		}
	}

	public boolean equals(Object other) {
		return (other instanceof MessageHistory
				&& this.events.equals(((MessageHistory) other).events));
	}

	public int hashCode() {
		return 17 * this.events.hashCode();
	}

	/**
	 * Returns a String representation of the history event list.
	 */
	public String toString() {
		return this.events.toString();
	}

}
