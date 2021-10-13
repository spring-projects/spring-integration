/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ManagedResource
public abstract class AbstractMessageGroupStore extends AbstractBatchingMessageGroupStore
		implements MessageGroupStore, Iterable<MessageGroup> {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<>();

	private final MessageGroupFactory persistentMessageGroupFactory =
			new SimpleMessageGroupFactory(SimpleMessageGroupFactory.GroupType.PERSISTENT);

	private boolean lazyLoadMessageGroups = true;

	private boolean timeoutOnIdle;

	protected AbstractMessageGroupStore() {
	}

	protected AbstractMessageGroupStore(boolean lazyLoadMessageGroups) {
		this.lazyLoadMessageGroups = lazyLoadMessageGroups;
	}

	@Override
	protected MessageGroupFactory getMessageGroupFactory() {
		if (this.lazyLoadMessageGroups) {
			return this.persistentMessageGroupFactory;
		}
		else {
			return super.getMessageGroupFactory();
		}
	}

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerMessageGroupExpiryCallback(MessageGroupCallback)}.
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		expiryCallbacks.forEach(this::registerMessageGroupExpiryCallback);
	}

	public boolean isTimeoutOnIdle() {
		return this.timeoutOnIdle;
	}

	/**
	 * Allows you to override the rule for the timeout calculation. Typical timeout is based from the time
	 * the {@link MessageGroup} was created. If you want the timeout to be based on the time
	 * the {@link MessageGroup} was idling (e.g., inactive from the last update) invoke this method with 'true'.
	 * Default is 'false'.
	 * @param timeoutOnIdle The boolean.
	 */
	public void setTimeoutOnIdle(boolean timeoutOnIdle) {
		this.timeoutOnIdle = timeoutOnIdle;
	}

	/**
	 * Specify if the result of the {@link #getMessageGroup(Object)} should be wrapped
	 * to the {@link PersistentMessageGroup} - a lazy-load proxy for messages in group
	 * Defaults to {@code true}.
	 * <p> The target logic is based on the {@link SimpleMessageGroupFactory.GroupType#PERSISTENT}.
	 * @param lazyLoadMessageGroups the {@code boolean} flag to use.
	 * @since 4.3
	 */
	public void setLazyLoadMessageGroups(boolean lazyLoadMessageGroups) {
		this.lazyLoadMessageGroups = lazyLoadMessageGroups;
	}

	@Override
	public void registerMessageGroupExpiryCallback(MessageGroupCallback callback) {
		if (callback instanceof UniqueExpiryCallback) {
			boolean uniqueExpiryCallbackPresent =
					this.expiryCallbacks.stream()
							.anyMatch(UniqueExpiryCallback.class::isInstance);

			if (uniqueExpiryCallbackPresent) {
				this.logger.error("Only one instance of 'UniqueExpiryCallback' can be registered in the " +
						"'MessageGroupStore'. Use a separate 'MessageGroupStore' for each aggregator/resequencer.");
			}
		}

		this.expiryCallbacks.add(callback);
	}

	@Override
	@ManagedOperation
	public synchronized int expireMessageGroups(long timeout) {
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
			count++;
		}
		return count;
	}

	@Override
	public MessageGroupMetadata getGroupMetadata(Object groupId) {
		throw new UnsupportedOperationException("Not yet implemented for this store");
	}

	@Override
	public void removeMessagesFromGroup(Object key, Message<?>... messages) {
		removeMessagesFromGroup(key, Arrays.asList(messages));
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		addMessagesToGroup(groupId, message);
		return getMessageGroup(groupId);
	}

	private void expire(MessageGroup group) {

		RuntimeException exception = null;

		for (MessageGroupCallback callback : this.expiryCallbacks) {
			try {
				callback.execute(this, group);
			}
			catch (RuntimeException e) {
				if (exception == null) {
					exception = e;
				}
				this.logger.error("Exception in expiry callback", e);
			}
		}

		if (exception != null) {
			throw exception;
		}
	}

}
