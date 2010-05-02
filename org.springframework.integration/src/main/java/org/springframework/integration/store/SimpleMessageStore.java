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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.util.UpperBound;
import org.springframework.util.Assert;

/**
 * Map-based implementation of {@link MessageStore} that enforces a maximum
 * capacity.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 2.0
 */
public class SimpleMessageStore implements MessageStore {

	private final ConcurrentMap<UUID, Message<?>> idToMessage;

	private final ConcurrentMap<Object, Collection<Message<?>>> correlationToMessage;

	private final UpperBound upperBound;

	/**
	 * Creates a SimpleMessageStore with a maximum size limited by the given
	 * capacity, or unlimited size if the given capacity is less than 1.
	 */
	public SimpleMessageStore(int capacity) {
		this.idToMessage = new ConcurrentHashMap<UUID, Message<?>>();
		this.correlationToMessage = new ConcurrentHashMap<Object, Collection<Message<?>>>();
		this.upperBound = new UpperBound(capacity);
	}

	/**
	 * Creates a SimpleMessageStore with unlimited capacity
	 */
	public SimpleMessageStore() {
		this(0);
	}

	@SuppressWarnings("unchecked")
	public <T> Message<T> put(Message<T> message) {
		if (!upperBound.tryAcquire(0)) {
			throw new MessagingException(this.getClass().getSimpleName()
					+ " was out of capacity at, try constructing it with a larger capacity.");
		}
		Object correlationId = message.getHeaders().getCorrelationId();
		if (correlationId!=null) {
			getMessagesInternal(correlationId).add(message);
		}
		return (Message<T>) this.idToMessage.put(message.getHeaders().getId(), message);
	}

	public Message<?> get(UUID key) {
		return (key != null) ? this.idToMessage.get(key) : null;
	}

	public Message<?> delete(UUID key) {
		if (key != null) {
			upperBound.release();
			return this.idToMessage.remove(key);
		}
		else
			return null;
	}

	public int size() {
		return this.idToMessage.size();
	}

	public Collection<Message<?>> list(Object correlationId) {
		Assert.notNull(correlationId, "'correlationKey' must not be null");
		Collection<Message<?>> collection = correlationToMessage.get(correlationId);
		if (collection==null) {
			return Collections.emptySet();
		}
		return new HashSet<Message<?>>(collection);
	}

	public void put(Object correlationId, Message<?> message) {
		getMessagesInternal(correlationId).add(message);
	}

	public Message<?> mark(Object correlationId, UUID messageId) {
		if (!correlationToMessage.containsKey(correlationId)) {
			return null;
		}
		Collection<Message<?>> messages = getMessagesInternal(correlationId);
		Message<?> result = null;
		for (Iterator<Message<?>> iterator = messages.iterator(); iterator.hasNext();) {
			Message<?> message = (Message<?>) iterator.next();
			if (message.getHeaders().getId().equals(messageId)) {
				iterator.remove();
				result = MessageBuilder.fromMessage(message).setHeader(PROCESSED, true).build();
			}
		}
		messages.add(result);
		return result;
	}

	public void deleteAll(Object correlationId) {
		correlationToMessage.remove(correlationId);
	}

	private Collection<Message<?>> getMessagesInternal(Object correlationId) {
		if (!correlationToMessage.containsKey(correlationId)) {
			correlationToMessage.putIfAbsent(correlationId, new HashSet<Message<?>>());
		}
		Collection<Message<?>> collection = correlationToMessage.get(correlationId);
		return collection;
	}

}
