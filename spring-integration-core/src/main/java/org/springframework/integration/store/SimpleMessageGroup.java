/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.store;

import org.springframework.integration.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a mutable group of correlated messages that is bound to a certain {@link MessageStore} and group id. The
 * group will grow during its lifetime, when messages are <code>add</code>ed to it. This MessageGroup is thread safe.
 * 
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @since 2.0
 */
public class SimpleMessageGroup implements MessageGroup {

	private final Object groupId;

	// Guards(marked, unmarked)
	private final Object lock = new Object();

	// @GuardedBy(lock)
	public final BlockingQueue<Message<?>> marked = new LinkedBlockingQueue<Message<?>>();

	// @GuardedBy(lock)
	public final BlockingQueue<Message<?>> unmarked = new LinkedBlockingQueue<Message<?>>();

	private final long timestamp;
	
	private volatile boolean complete;

	public SimpleMessageGroup(Object groupId) {
		this(Collections.<Message<?>> emptyList(), Collections.<Message<?>> emptyList(), groupId, System
				.currentTimeMillis());
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> unmarked, Object groupId) {
		this(unmarked, Collections.<Message<?>> emptyList(), groupId, System.currentTimeMillis());
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> unmarked, Collection<? extends Message<?>> marked,
			Object groupId, long timestamp) {
		this.groupId = groupId;
		this.timestamp = timestamp;
		synchronized (lock) {
			for (Message<?> message : unmarked) {
				addUnmarked(message);
			}
			for (Message<?> message : marked) {
				addMarked(message);
			}
		}
	}

	public SimpleMessageGroup(MessageGroup template) {
		this.groupId = template.getGroupId();
		this.complete = template.isComplete();
		synchronized (lock) {
			// Explicit iteration to work around bug in JDK (before 1.6.0_20
			for (Message<?> message : template.getMarked()) {
				if (message != null) {
					this.marked.add(message);
				}
			}
			for (Message<?> message : template.getUnmarked()) {
				if (message != null) {
					this.unmarked.add(message);
				}
			}
		}
		this.timestamp = template.getTimestamp();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean canAdd(Message<?> message) {
		return !isMember(message);
	}

	public void add(Message<?> message) {
		addUnmarked(message);
	}

	public void remove(Message<?> message) {
		synchronized (lock) {
			marked.remove(message);
			unmarked.remove(message);
		}
	}

	private boolean addUnmarked(Message<?> message) {
		if (isMember(message)) {
			return false;
		}
		synchronized (lock) {
			return this.unmarked.offer(message);
		}
	}

	private boolean addMarked(Message<?> message) {
		if (isMember(message)) {
			return false;
		}
		synchronized (lock) {
			return this.marked.offer(message);
		}
	}

	public Collection<Message<?>> getUnmarked() {
		synchronized (lock) {
			return Collections.unmodifiableCollection(unmarked);
		}
	}

	public Collection<Message<?>> getMarked() {
		synchronized (lock) {
			return Collections.unmodifiableCollection(marked);
		}
	}

	public Object getGroupId() {
		return groupId;
	}

	public boolean isComplete() {
		return this.complete;
	}
	
	public void complete() {
		this.complete = true;
	}
	
	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return getOne().getHeaders().getSequenceSize();
	}

	/**
	 * Mark the given message in this group. If the message is not part of this group then this call has no effect.
	 */
	public void mark(Message<?> messageToMark) {
		synchronized (lock) {
			unmarked.remove(messageToMark);
			marked.offer(messageToMark);
		}
	}

	public void markAll() {
		synchronized (lock) {
			unmarked.drainTo(marked);
		}
	}

	public int size() {
		synchronized (lock) {
			return marked.size() + unmarked.size();
		}
	}

	public Message<?> getOne() {
		Message<?> one = unmarked.peek();
		if (one == null) {
			one = marked.peek();
		}
		return one;
	}
	
	public void cleanup(){
		this.marked.clear();
		this.unmarked.clear();
	}

	/**
	 * This method determines whether messages have been added to this group that supersede the given message based on
	 * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
	 * or sequences that are missing certain sequence numbers.
	 */
	private boolean isMember(Message<?> message) {
		if (size() == 0) {
			return false;
		}
		Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
		if (messageSequenceNumber != null && messageSequenceNumber > 0) {
			Integer messageSequenceSize = message.getHeaders().getSequenceSize();
			if (!messageSequenceSize.equals(getSequenceSize())) {
				return true;
			}
			else {
				synchronized (lock) {
					if (containsSequenceNumber(unmarked, messageSequenceNumber)
							|| containsSequenceNumber(marked, messageSequenceNumber)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean containsSequenceNumber(Collection<Message<?>> messages, Integer messageSequenceNumber) {
		for (Message<?> member : messages) {
			Integer memberSequenceNumber = member.getHeaders().getSequenceNumber();
			if (messageSequenceNumber.equals(memberSequenceNumber)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "SimpleMessageGroup{" +
				"groupId=" + groupId +
				", lock=" + lock +
				", marked=" + marked +
				", unmarked=" + unmarked +
				", timestamp=" + timestamp +
				'}';
	}
}
