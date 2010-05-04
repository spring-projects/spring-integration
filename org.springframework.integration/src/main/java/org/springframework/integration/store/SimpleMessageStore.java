/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

	private final ConcurrentMap<UUID, Message<?>> idToMessage;

	private final ConcurrentMap<Object, SimpleMessageGroup> correlationToMessageGroup;

	private final UpperBound upperBound;

	/**
	 * Creates a SimpleMessageStore with a maximum size limited by the given capacity, or unlimited size if the given
	 * capacity is less than 1.
	 */
	public SimpleMessageStore(int capacity) {
		this.idToMessage = new ConcurrentHashMap<UUID, Message<?>>();
		this.correlationToMessageGroup = new ConcurrentHashMap<Object, SimpleMessageGroup>();
		this.upperBound = new UpperBound(capacity);
	}

	/**
	 * Creates a SimpleMessageStore with unlimited capacity
	 */
	public SimpleMessageStore() {
		this(0);
	}

	public <T> Message<T> addMessage(Message<T> message) {
		if (!upperBound.tryAcquire(0)) {
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
			upperBound.release();
			return this.idToMessage.remove(key);
		}
		else
			return null;
	}

	public MessageGroup getMessageGroup(Object correlationId) {
		Assert.notNull(correlationId, "'correlationKey' must not be null");
		MessageGroup collection = correlationToMessageGroup.get(correlationId);
		if (collection == null) {
			return new SimpleMessageGroup(correlationId);
		}
		return new SimpleMessageGroup(collection);
	}

	public void addMessageToGroup(Object correlationId, Message<?> message) {
		getMessageGroupInternal(correlationId).add(message);
	}
	
	public void markMessageGroup(MessageGroup group) {		
		Object correlationId = group.getCorrelationKey();
		MessageGroup internal = getMessageGroupInternal(correlationId);
		internal.mark();
		group.mark();	
	}

	public void removeMessageGroup(Object correlationId) {
		correlationToMessageGroup.remove(correlationId);
	}

	private MessageGroup getMessageGroupInternal(Object correlationId) {
		if (!correlationToMessageGroup.containsKey(correlationId)) {
			correlationToMessageGroup.putIfAbsent(correlationId, new SimpleMessageGroup(correlationId));
		}
		return correlationToMessageGroup.get(correlationId);
	}

}
