/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Represents a mutable group of correlated messages that is bound to a certain {@link MessageStore} and group id.
 * The group will grow during its lifetime, when messages are <code>add</code>ed to it.
 * This MessageGroup is thread safe.
 *
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SimpleMessageGroup implements MessageGroup {

	private final Object groupId;

	private final Collection<Message<?>> messages;

	private final long timestamp;

	private volatile int lastReleasedMessageSequence;

	private volatile long lastModified;

	private volatile boolean complete;

	public SimpleMessageGroup(Object groupId) {
		this(Collections.<Message<?>>emptyList(), groupId);
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> messages, Object groupId) {
		this(messages, groupId, System.currentTimeMillis(), false);
	}

	public SimpleMessageGroup(MessageGroup messageGroup) {
		this(messageGroup.getMessages(), messageGroup.getGroupId(), messageGroup.getTimestamp(),
				messageGroup.isComplete());
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> messages, Object groupId, long timestamp,
	                          boolean complete) {
		this(new LinkedHashSet<Message<?>>(), messages, groupId, timestamp, complete);
	}

	SimpleMessageGroup(Collection<Message<?>> internalStore, Collection<? extends Message<?>> messages, Object groupId,
	                   long timestamp, boolean complete) {
		Assert.notNull(internalStore, "'internalStore' must not be null");
		Assert.notNull(messages, "'messages' must not be null");
		this.messages = internalStore;
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.complete = complete;
		for (Message<?> message : messages) {
			if (message != null) { //see INT-2666
				addMessage(message);
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
		return this.messages.remove(message);
	}

	@Override
	public int getLastReleasedMessageSequenceNumber() {
		return this.lastReleasedMessageSequence;
	}

	private boolean addMessage(Message<?> message) {
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
	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return new IntegrationMessageHeaderAccessor(getOne()).getSequenceSize();
	}

	@Override
	public int size() {
		return this.messages.size();
	}

	@Override
	public Message<?> getOne() {
		synchronized (this.messages) {
			Iterator<Message<?>> iterator = this.messages.iterator();
			return iterator.hasNext() ? iterator.next() : null;
		}
	}

	@Override
	public void clear() {
		this.messages.clear();
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
