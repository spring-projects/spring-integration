/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.store;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.messaging.Message;

/**
 * Represents a mutable group of correlated messages that is bound to a certain {@link MessageStore} and group id.
 * The group will grow during its lifetime, when messages are {@link #add}ed to it.
 * This MessageGroup is thread safe.
 *
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0
 */
public class SimpleMessageGroup implements MessageGroup {

	private final Lock lock = new ReentrantLock();

	private final Object groupId;

	private final Collection<Message<?>> messages;

	private final Set<Integer> sequences = new HashSet<>();

	private final long timestamp;

	private volatile int lastReleasedMessageSequence;

	private volatile long lastModified;

	private volatile boolean complete;

	private volatile @Nullable String condition;

	public SimpleMessageGroup(Object groupId) {
		this(Collections.emptyList(), groupId);
	}

	public SimpleMessageGroup(Collection<? extends @Nullable Message<?>> messages, Object groupId) {
		this(messages, groupId, System.currentTimeMillis(), false);
	}

	public SimpleMessageGroup(MessageGroup messageGroup) {
		this(messageGroup.getMessages(), messageGroup.getGroupId(), messageGroup.getTimestamp(),
				messageGroup.isComplete());
	}

	public SimpleMessageGroup(Collection<? extends @Nullable Message<?>> messages, Object groupId, long timestamp,
			boolean complete) {
		this(new LinkedHashSet<>(), messages, groupId, timestamp, complete, false);
	}

	public SimpleMessageGroup(Collection<Message<?>> internalStore,
			@Nullable Collection<? extends @Nullable Message<?>> messages,
			Object groupId, long timestamp, boolean complete, boolean storePreLoaded) {

		this.messages = internalStore;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.complete = complete;
		if (!storePreLoaded && messages != null) {
			for (Message<?> message : messages) {
				if (message != null) {
					addMessage(message);
				}
			}
		}
	}

	@Override
	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public long getLastModified() {
		return this.lastModified;
	}

	@Override
	public boolean canAdd(Message<?> message) {
		return true;
	}

	@Override
	public void add(Message<?> messageToAdd) {
		addMessage(messageToAdd);
	}

	@Override
	public boolean remove(Message<?> message) {
		this.sequences.remove(StaticMessageHeaderAccessor.getSequenceNumber(message));
		return this.messages.remove(message);
	}

	@Override
	public int getLastReleasedMessageSequenceNumber() {
		return this.lastReleasedMessageSequence;
	}

	private boolean addMessage(Message<?> message) {
		this.sequences.add(StaticMessageHeaderAccessor.getSequenceNumber(message));
		return this.messages.add(message);
	}

	@Override
	public Collection<Message<?>> getMessages() {
		return Collections.unmodifiableCollection(this.messages);
	}

	@Override
	public void setLastReleasedMessageSequenceNumber(int sequenceNumber) {
		this.lastReleasedMessageSequence = sequenceNumber;
	}

	@Override
	public Object getGroupId() {
		return this.groupId;
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}

	@Override
	public void complete() {
		this.complete = true;
	}

	@Override
	@SuppressWarnings("NullAway") // if 'size() > 0', then 'getOne' is not null
	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return StaticMessageHeaderAccessor.getSequenceSize(getOne());
	}

	@Override
	public int size() {
		return this.messages.size();
	}

	@Override
	public void setCondition(@Nullable String condition) {
		this.condition = condition;
	}

	@Override
	public @Nullable String getCondition() {
		return this.condition;
	}

	@Override
	public @Nullable Message<?> getOne() {
		this.lock.lock();
		try {
			Iterator<Message<?>> iterator = this.messages.iterator();
			return iterator.hasNext() ? iterator.next() : null;
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public void clear() {
		this.messages.clear();
		this.sequences.clear();
	}

	/**
	 * Return true if a message with this sequence number header exists in
	 * the group.
	 * @param sequence the sequence number.
	 * @return true if it exists.
	 * @since 4.3.7
	 */
	public boolean containsSequence(Integer sequence) {
		return this.sequences.contains(sequence);
	}

	@Override
	public String toString() {
		return "SimpleMessageGroup{" +
				"groupId=" + this.groupId +
				", messages=" + this.messages +
				", timestamp=" + this.timestamp +
				", lastModified=" + this.lastModified +
				'}';
	}

}
