/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 4.3
 */
class PersistentMessageGroup implements MessageGroup {

	private static final Log LOGGER = LogFactory.getLog(PersistentMessageGroup.class);

	private final MessageGroupStore messageGroupStore;

	private final Collection<Message<?>> messages = new PersistentCollection();

	private final MessageGroup original;

	private volatile Message<?> oneMessage;

	private volatile int size;

	PersistentMessageGroup(MessageGroupStore messageGroupStore, MessageGroup original) {
		this.messageGroupStore = messageGroupStore;
		this.original = original;
	}

	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public Collection<Message<?>> getMessages() {
		return Collections.unmodifiableCollection(this.messages);
	}

	/**
	 * The resulting {@link Stream} must be closed after use,
	 * it can be declared as a resource in a {@code try-with-resources} statement.
	 * @return the stream of messages in this group.
	 */
	@Override
	public Stream<Message<?>> streamMessages() {
		return this.messageGroupStore.streamMessagesForGroup(this.original.getGroupId());
	}

	@Override
	public Message<?> getOne() {
		if (this.oneMessage == null) {
			synchronized (this) {
				if (this.oneMessage == null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Lazy loading of one message for messageGroup: " + this.original.getGroupId());
					}
					this.oneMessage = this.messageGroupStore.getOneMessageFromGroup(this.original.getGroupId());
				}
			}
		}
		return this.oneMessage;
	}

	@Override
	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		else {
			Message<?> message = getOne();
			if (message != null) {
				Integer sequenceSize = message.getHeaders()
						.get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
				return (sequenceSize != null ? sequenceSize : 0);
			}
			else {
				return 0;
			}
		}
	}

	@Override
	public int size() {
		if (this.size == 0) {
			synchronized (this) {
				if (this.size == 0) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Lazy loading of group size for messageGroup: " + this.original.getGroupId());
					}
					this.size = this.messageGroupStore.messageGroupSize(this.original.getGroupId());
				}
			}
		}
		return this.size;
	}

	@Override
	public Object getGroupId() {
		return this.original.getGroupId();
	}

	@Override
	public boolean canAdd(Message<?> message) {
		return this.original.canAdd(message);
	}

	@Override
	public int getLastReleasedMessageSequenceNumber() {
		return this.original.getLastReleasedMessageSequenceNumber();
	}

	@Override
	public boolean isComplete() {
		return this.original.isComplete();
	}

	@Override
	public void complete() {
		this.original.complete();
	}

	@Override
	public long getTimestamp() {
		return this.original.getTimestamp();
	}

	@Override
	public long getLastModified() {
		return this.original.getLastModified();
	}

	@Override
	public void setLastModified(long lastModified) {
		this.original.setLastModified(lastModified);
	}

	@Override
	public void add(Message<?> messageToAdd) {
		this.original.add(messageToAdd);
	}

	@Override
	public boolean remove(Message<?> messageToRemove) {
		return this.original.remove(messageToRemove);
	}

	@Override
	public void setLastReleasedMessageSequenceNumber(int sequenceNumber) {
		this.original.setLastReleasedMessageSequenceNumber(sequenceNumber);
	}

	@Override
	public void setCondition(String condition) {
		this.original.setCondition(condition);
	}

	@Override
	@Nullable
	public String getCondition() {
		return this.original.getCondition();
	}

	@Override
	public void clear() {
		this.original.clear();
	}


	private final class PersistentCollection extends AbstractCollection<Message<?>> {

		private volatile Collection<Message<?>> collection;

		PersistentCollection() {
		}

		private void load() {
			if (this.collection == null) {
				synchronized (this) {
					if (this.collection == null) {
						Object groupId = PersistentMessageGroup.this.original.getGroupId();
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Lazy loading of messages for messageGroup: " + groupId);
						}
						this.collection = PersistentMessageGroup.this.messageGroupStore.getMessagesForGroup(groupId);
					}
				}
			}
		}

		@Override
		public boolean contains(Object o) {
			load();
			return this.collection.contains(o);
		}

		@Override
		public Object[] toArray() {
			load();
			return this.collection.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			load();
			return this.collection.toArray(a);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			load();
			return this.collection.containsAll(c);
		}

		@Override
		public Iterator<Message<?>> iterator() {
			load();
			return this.collection.iterator();
		}

		@Override
		public int size() {
			return PersistentMessageGroup.this.size();
		}

		@Override
		public Spliterator<Message<?>> spliterator() {
			return streamMessages().spliterator();
		}

		@Override
		public Stream<Message<?>> stream() {
			return streamMessages();
		}

	}

}
