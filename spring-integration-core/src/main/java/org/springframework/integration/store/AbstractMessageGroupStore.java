/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.CheckedCallable;
import org.springframework.integration.util.CheckedRunnable;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Youbin Wu
 *
 * @since 2.0
 */
@ManagedResource
public abstract class AbstractMessageGroupStore extends AbstractBatchingMessageGroupStore
		implements MessageGroupStore, Iterable<MessageGroup> {

	protected static final String INTERRUPTED_WHILE_OBTAINING_LOCK = "Interrupted while obtaining lock";

	protected static final String GROUP_ID_MUST_NOT_BE_NULL = "'groupId' must not be null";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final Lock lock = new ReentrantLock();

	private final Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<>();

	private final MessageGroupFactory persistentMessageGroupFactory =
			new SimpleMessageGroupFactory(SimpleMessageGroupFactory.GroupType.PERSISTENT);

	private boolean lazyLoadMessageGroups = true;

	private boolean timeoutOnIdle;

	private LockRegistry lockRegistry = new DefaultLockRegistry();

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

	/**
	 * Specify the type of the {@link LockRegistry} to ensure atomic operations
	 * @param lockRegistry lockRegistryType
	 * @since 6.5
	 */
	public final void setLockRegistry(LockRegistry lockRegistry) {
		Assert.notNull(lockRegistry, "The LockRegistry cannot be null");
		this.lockRegistry = lockRegistry;
	}

	protected LockRegistry getLockRegistry() {
		return this.lockRegistry;
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
	public int expireMessageGroups(long timeout) {
		this.lock.lock();
		try {
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
		finally {
			this.lock.unlock();
		}
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
	public void removeMessagesFromGroup(Object key, Collection<Message<?>> messages) {
		Assert.notNull(key, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(key, () -> doRemoveMessagesFromGroup(key, messages));
	}

	protected abstract void doRemoveMessagesFromGroup(Object key, Collection<Message<?>> messages);

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(groupId, () -> doAddMessagesToGroup(groupId, messages));
	}

	protected abstract void doAddMessagesToGroup(Object groupId, Message<?>... messages);

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		addMessagesToGroup(groupId, message);
		return getMessageGroup(groupId);
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(groupId, () -> doRemoveMessageGroup(groupId));
	}

	protected abstract void doRemoveMessageGroup(Object groupId);

	@Override
	public boolean removeMessageFromGroupById(Object groupId, UUID messageId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		return executeLocked(groupId, () -> doRemoveMessageFromGroupById(groupId, messageId));
	}

	protected boolean doRemoveMessageFromGroupById(Object groupId, UUID messageId) {
		throw new UnsupportedOperationException("Not supported for this store");
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(groupId, () -> doSetLastReleasedSequenceNumberForGroup(groupId, sequenceNumber));
	}

	protected abstract void doSetLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber);

	@Override
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(groupId, () -> doCompleteGroup(groupId));
	}

	protected abstract void doCompleteGroup(Object groupId);

	@Override
	public void setGroupCondition(Object groupId, String condition) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		executeLocked(groupId, () -> doSetGroupCondition(groupId, condition));
	}

	protected abstract void doSetGroupCondition(Object groupId, String condition);

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		return executeLocked(groupId, () -> doPollMessageFromGroup(groupId));
	}

	protected abstract Message<?> doPollMessageFromGroup(Object groupId);

	protected <T, E extends RuntimeException> T executeLocked(Object groupId, CheckedCallable<T, E> runnable) {
		try {
			return this.lockRegistry.executeLocked(groupId, runnable);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(INTERRUPTED_WHILE_OBTAINING_LOCK, ex);
		}
	}

	protected <E extends RuntimeException> void executeLocked(Object groupId, CheckedRunnable<E> runnable) {
		try {
			this.lockRegistry.executeLocked(groupId, runnable);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(INTERRUPTED_WHILE_OBTAINING_LOCK, ex);
		}
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
