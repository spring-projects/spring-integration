/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.util.UpperBound;
import org.springframework.util.Assert;

/**
 * Map-based implementation of {@link MessageStore} and {@link MessageGroupStore}. Enforces a maximum capacity for the
 * store.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Dave Syer
 * 
 * @since 2.0
 */
public class SimpleMessageStore implements MessageStore, MessageGroupStore {

	private static final Log logger = LogFactory.getLog(SimpleMessageStore.class);

	private final ConcurrentMap<UUID, Message<?>> idToMessage;

	private final ConcurrentMap<Object, SimpleMessageGroup> correlationToMessageGroup;

	private final UpperBound individualUpperBound;

	private final UpperBound groupUpperBound;

	private Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<MessageGroupCallback>();

	/**
	 * Creates a SimpleMessageStore with a maximum size limited by the given capacity, or unlimited size if the given
	 * capacity is less than 1. The capacities are applied independently to messages stored via
	 * {@link #addMessage(Message)} and to those stored via {@link #addMessageToGroup(Object, Message)}. In both cases
	 * the capacity applies to the number of messages that can be stored, and once that limit is reached attempting to
	 * store another will result in an exception.
	 */
	public SimpleMessageStore(int individualCapacity, int groupCapacity) {
		this.idToMessage = new ConcurrentHashMap<UUID, Message<?>>();
		this.correlationToMessageGroup = new ConcurrentHashMap<Object, SimpleMessageGroup>();
		this.individualUpperBound = new UpperBound(individualCapacity);
		this.groupUpperBound = new UpperBound(groupCapacity);
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

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerExpiryCallback(MessageGroupCallback)}.
	 * 
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		for (MessageGroupCallback callback : expiryCallbacks) {
			registerExpiryCallback(callback);
		}
	}

	public <T> Message<T> addMessage(Message<T> message) {
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
		} else
			return null;
	}

	public MessageGroup getMessageGroup(Object correlationId) {
		Assert.notNull(correlationId, "'correlationKey' must not be null");
		SimpleMessageGroup group = correlationToMessageGroup.get(correlationId);
		if (group == null) {
			return new SimpleMessageGroup(correlationId);
		}
		return new SimpleMessageGroup(group);
	}

	public void addMessageToGroup(Object correlationId, Message<?> message) {
		if (!groupUpperBound.tryAcquire(0)) {
			throw new MessagingException(this.getClass().getSimpleName()
					+ " was out of capacity at, try constructing it with a larger capacity.");
		}
		getMessageGroupInternal(correlationId).add(message);
	}

	public void markMessageGroup(MessageGroup group) {
		Object correlationId = group.getCorrelationKey();
		MessageGroup internal = getMessageGroupInternal(correlationId);
		internal.mark();
		group.mark();
	}

	public void removeMessageGroup(Object correlationId) {
		groupUpperBound.release();
		correlationToMessageGroup.remove(correlationId);
	}

	public void registerExpiryCallback(MessageGroupCallback callback) {
		expiryCallbacks.add(callback);
	}

	public int expireMessageGroups(long timeout) {
		int count = 0;
		long threshold = System.currentTimeMillis() - timeout;
		for (MessageGroup group : correlationToMessageGroup.values()) {
			if (group.getTimestamp() < threshold) {
				count++;
				expire(group);
				removeMessageGroup(group.getCorrelationKey());
			}
		}
		return count;
	}

	private void expire(MessageGroup group) {

		RuntimeException exception = null;

		for (MessageGroupCallback callback : expiryCallbacks) {
			try {
				callback.execute(group);
			} catch (RuntimeException e) {
				if (exception == null) {
					exception = e;
				}
				logger.error("Exception in expiry callback", e);
			}
		}

		if (exception != null) {
			throw exception;
		}

	}

	private SimpleMessageGroup getMessageGroupInternal(Object correlationId) {
		if (!correlationToMessageGroup.containsKey(correlationId)) {
			correlationToMessageGroup.putIfAbsent(correlationId, new SimpleMessageGroup(correlationId));
		}
		return correlationToMessageGroup.get(correlationId);
	}

}
