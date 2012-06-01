/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.util.DefaultLockRegistry;
import org.springframework.integration.util.LockRegistry;
import org.springframework.integration.util.UpperBound;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Map-based in-memory implementation of {@link MessageStore} and {@link MessageGroupStore}. Enforces a maximum capacity for the
 * store.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 2.0
 */
@ManagedResource
public class SimpleMessageStore extends AbstractMessageGroupStore implements MessageStore, MessageGroupStore {

	private volatile LockRegistry lockRegistry;

	private final ConcurrentMap<UUID, Message<?>> idToMessage;

	private final ConcurrentMap<Object, SimpleMessageGroup> groupIdToMessageGroup;

	private final UpperBound individualUpperBound;

	private final UpperBound groupUpperBound;

	private volatile boolean isUsed;

	/**
	 * Creates a SimpleMessageStore with a maximum size limited by the given capacity, or unlimited size if the given
	 * capacity is less than 1. The capacities are applied independently to messages stored via
	 * {@link #addMessage(Message)} and to those stored via {@link #addMessageToGroup(Object, Message)}. In both cases
	 * the capacity applies to the number of messages that can be stored, and once that limit is reached attempting to
	 * store another will result in an exception.
	 */
	public SimpleMessageStore(int individualCapacity, int groupCapacity) {
		this(individualCapacity, groupCapacity, new DefaultLockRegistry());
	}

	/**
	 * See {@link #SimpleMessageStore(int, int, LockRegistry)}.
	 * Also allows the provision of a custom {@link LockRegistry}
	 * rather than using the default.
	 */
	public SimpleMessageStore(int individualCapacity, int groupCapacity, LockRegistry lockRegistry) {
		Assert.notNull(lockRegistry, "The LockRegistry cannot be null");
		this.idToMessage = new ConcurrentHashMap<UUID, Message<?>>();
		this.groupIdToMessageGroup = new ConcurrentHashMap<Object, SimpleMessageGroup>();
		this.individualUpperBound = new UpperBound(individualCapacity);
		this.groupUpperBound = new UpperBound(groupCapacity);
		this.lockRegistry = lockRegistry;
	}

	/**
	 * Creates a SimpleMessageStore with the same capacity for individual and grouped messages.
	 */
	public SimpleMessageStore(int capacity) {
		this(capacity, capacity);
	}

	/**
	 * Creates a SimpleMessageStore with unlimited capacity
	 */
	public SimpleMessageStore() {
		this(0);
	}

	public void setLockRegistry(LockRegistry lockRegistry) {
		Assert.notNull(lockRegistry, "The LockRegistry cannot be null");
		Assert.isTrue(!(this.isUsed), "Cannot change the lock registry after the store has been used");
		this.lockRegistry = lockRegistry;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return idToMessage.size();
	}

	public <T> Message<T> addMessage(Message<T> message) {
		this.isUsed = true;
		if (!individualUpperBound.tryAcquire(0)) {
			throw new MessagingException(this.getClass().getSimpleName()
					+ " was out of capacity at, try constructing it with a larger capacity.");
		}
		this.idToMessage.put(message.getHeaders().getId(), message);
		return message;
	}

	public Message<?> getMessage(UUID key) {
		return (key != null) ? this.idToMessage.get(key) : null;
	}

	public Message<?> removeMessage(UUID key) {
		if (key != null) {
			individualUpperBound.release();
			return this.idToMessage.remove(key);
		}
		else
			return null;
	}

	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		SimpleMessageGroup group = groupIdToMessageGroup.get(groupId);
		if (group == null) {
			return new SimpleMessageGroup(groupId);
		}
		return new SimpleMessageGroup(group);
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		if (!groupUpperBound.tryAcquire(0)) {
			throw new MessagingException(this.getClass().getSimpleName()
					+ " was out of capacity at, try constructing it with a larger capacity.");
		}
		Lock lock = this.lockRegistry.obtain(groupId);
		try {
			lock.lockInterruptibly();
			try {
				SimpleMessageGroup group = this.groupIdToMessageGroup.get(groupId);
				if (group == null) {
					group = new SimpleMessageGroup(groupId);
					this.groupIdToMessageGroup.putIfAbsent(groupId, group);
				}
				group.add(message);
				return group;
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while obtaining lock", e);
		}
	}

	public void removeMessageGroup(Object groupId) {
		Lock lock = this.lockRegistry.obtain(groupId);
		try {
			lock.lockInterruptibly();
			try {
				if (!groupIdToMessageGroup.containsKey(groupId)) {
					return;
				}

				groupUpperBound.release(groupIdToMessageGroup.get(groupId).size());
				groupIdToMessageGroup.remove(groupId);
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while obtaining lock", e);
		}
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Lock lock = this.lockRegistry.obtain(groupId);
		try {
			lock.lockInterruptibly();
			try {
				SimpleMessageGroup group = this.groupIdToMessageGroup.get(groupId);
				Assert.notNull(group, "MessageGroup for groupId '" + groupId + "' " +
						"can not be located while attempting to remove Message from the MessageGroup");
				group.remove(messageToRemove);
				return group;
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while obtaining lock", e);
		}
	}

	public Iterator<MessageGroup> iterator() {
		return new HashSet<MessageGroup>(groupIdToMessageGroup.values()).iterator();
	}

	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Lock lock = this.lockRegistry.obtain(groupId);
		try {
			lock.lockInterruptibly();
			try {
				SimpleMessageGroup group = this.groupIdToMessageGroup.get(groupId);
				Assert.notNull(group, "MessageGroup for groupId '" + groupId + "' " +
						"can not be located while attempting to set 'lastReleasedSequenceNumber'");
				group.setLastReleasedMessageSequenceNumber(sequenceNumber);
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while obtaining lock", e);
		}
	}

	public void completeGroup(Object groupId) {
		Lock lock = this.lockRegistry.obtain(groupId);
		try {
			lock.lockInterruptibly();
			try {
				SimpleMessageGroup group = this.groupIdToMessageGroup.get(groupId);
				Assert.notNull(group, "MessageGroup for groupId '" + groupId + "' " +
						"can not be located while attempting to complete the MessageGroup");
				group.complete();
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Interrupted while obtaining lock", e);
		}
	}

	public Message<?> pollMessageFromGroup(Object groupId) {
		Collection<Message<?>> messageList = this.getMessageGroup(groupId).getMessages();
		Message<?> message = null;
		if (!CollectionUtils.isEmpty(messageList)){
			message = messageList.iterator().next();
			if (message != null){
				this.removeMessageFromGroup(groupId, message);
			}
		}
		return message;
	}

	public int messageGroupSize(Object groupId) {
		return this.getMessageGroup(groupId).size();
	}
}
