/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
@ManagedResource
public abstract class AbstractMessageGroupStore implements MessageGroupStore, Iterable<MessageGroup>,
		BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<MessageGroupCallback>();

	private volatile boolean timeoutOnIdle;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	public AbstractMessageGroupStore() {
		super();
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return messageBuilderFactory;
	}

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerMessageGroupExpiryCallback(MessageGroupCallback)}.
	 *
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		for (MessageGroupCallback callback : expiryCallbacks) {
			registerMessageGroupExpiryCallback(callback);
		}
	}

	public boolean isTimeoutOnIdle() {
		return timeoutOnIdle;
	}

	/**
	 * Allows you to override the rule for the timeout calculation. Typical timeout is based from the time
	 * the {@link MessageGroup} was created. If you want the timeout to be based on the time
	 * the {@link MessageGroup} was idling (e.g., inactive from the last update) invoke this method with 'true'.
	 * Default is 'false'.
	 *
	 * @param timeoutOnIdle The boolean.
	 */
	public void setTimeoutOnIdle(boolean timeoutOnIdle) {
		this.timeoutOnIdle = timeoutOnIdle;
	}

	@Override
	public void registerMessageGroupExpiryCallback(MessageGroupCallback callback) {
		expiryCallbacks.add(callback);
	}

	@Override
	public int expireMessageGroups(long timeout) {
		int count = 0;
		long threshold = System.currentTimeMillis() - timeout;
		for (MessageGroup group : this) {

			long timestamp = group.getTimestamp();
			if (this.isTimeoutOnIdle() && group.getLastModified() > 0) {
			    timestamp = group.getLastModified();
			}

			if (timestamp <= threshold) {
				count++;
				expire(copy(group));
			}
		}
		return count;
	}

	/**
	 * Used by expireMessageGroups. We need to return a snapshot of the group
	 * at the time the reaper runs, so we can properly detect if the
	 * group changed between now and the attempt to expire the group.
	 * Not necessary for persistent stores, so the default behavior is
	 * to just return the group.
	 * @param group The group.
	 * @return The group, or a copy.
	 * @since 4.0.1
	 */
	protected MessageGroup copy(MessageGroup group) {
		return group;
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		int count = 0;
		for (MessageGroup group : this) {
			count += group.size();
		}
		return count;
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		int count = 0;
		for (@SuppressWarnings("unused") MessageGroup group : this) {
			count ++;
		}
		return count;
	}

	@Override
	public MessageGroupMetadata getGroupMetadata(Object groupId) {
		throw new UnsupportedOperationException("Not yet implemented for this store");
	}

	protected abstract Collection<Message<?>> getMessagesForGroup(Object groupId);

	private void expire(MessageGroup group) {

		RuntimeException exception = null;

		for (MessageGroupCallback callback : expiryCallbacks) {
			try {
				callback.execute(this, group);
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

	protected class PersistentMessageGroup implements MessageGroup {

		private final Collection<Message<?>> messages = new PersistentCollection();

		private final MessageGroup original;

		private volatile Message<?> oneMessage;

		private volatile int size;

		public PersistentMessageGroup(MessageGroup original) {
			this.original = original;
		}

		public void setSize(int size) {
			this.size = size;
		}

		@Override
		public Collection<Message<?>> getMessages() {
			return Collections.unmodifiableCollection(this.messages);
		}

		@Override
		public Message<?> getOne() {
			if (this.oneMessage == null) {
				synchronized (this) {
					if (this.oneMessage == null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Lazy loading of one message for messageGroup: " + this.original.getGroupId());
						}
						System.out.println("Lazy loading of one message for messageGroup: " + this.original.getGroupId());
						this.oneMessage = getOneMessageFromGroup(this.original.getGroupId());
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
					return message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
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
						if (logger.isDebugEnabled()) {
							logger.debug("Lazy loading of group size for messageGroup: " + this.original.getGroupId());
						}
						System.out.println("Lazy loading of group size for messageGroup: " + this.original.getGroupId());
						this.size = messageGroupSize(this.original.getGroupId());
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


		private class PersistentCollection extends AbstractCollection<Message<?>> {

			private volatile Collection<Message<?>> collection;

			private void load() {
				if (this.collection == null) {
					synchronized (this) {
						if (this.collection == null) {
							if (logger.isDebugEnabled()) {
								logger.debug("Lazy loading of messages for messageGroup: " + original.getGroupId());
							}
							System.out.println("Lazy loading of messages for messageGroup: " + original.getGroupId());
							this.collection = getMessagesForGroup(original.getGroupId());
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

		}

	}

}
