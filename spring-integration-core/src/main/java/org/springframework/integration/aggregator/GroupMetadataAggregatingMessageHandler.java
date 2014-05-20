/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class GroupMetadataAggregatingMessageHandler extends AggregatingMessageHandler {

	public GroupMetadataAggregatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
			CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {
		super(processor, store, correlationStrategy, releaseStrategy);
	}

	public GroupMetadataAggregatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		super(processor, store);
	}

	public GroupMetadataAggregatingMessageHandler(MessageGroupProcessor processor) {
		super(processor);
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object correlationKey = getCorrelationStrategy().getCorrelationKey(message);
		Assert.state(correlationKey != null, "Null correlation not allowed.  Maybe the CorrelationStrategy is failing?");

		if (logger.isDebugEnabled()) {
			logger.debug("Handling message with correlationKey [" + correlationKey + "]: " + message);
		}

		UUID groupIdUuid = UUIDConverter.getUUID(correlationKey);
		Lock lock = getLockRegistry().obtain(groupIdUuid.toString());

		lock.lockInterruptibly();
		try {
			ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(groupIdUuid);
			if (scheduledFuture != null) {
				boolean canceled = scheduledFuture.cancel(true);
				if (canceled && logger.isDebugEnabled()) {
					logger.debug("Cancel 'forceComplete' scheduling for MessageGroup with Correlation Key [ "
							+ correlationKey + "].");
				}
			}
			MessageGroupMetadata messageGroup = messageStore.getGroupMetadata(correlationKey);

			if (!messageGroup.isComplete()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding message to group [ " + messageGroup + "]");
				}
				messageGroup = this.messageStore.addMessageToGroupMetadata(correlationKey, message);

				MessageGroup groupProxy = new MessageGroupProxy(messageGroup, this.messageStore);

				if (getReleaseStrategy().canRelease(groupProxy)) {
					MessageGroup group = this.messageStore.getMessageGroup(correlationKey);
					Collection<Message<?>> completedMessages = null;
					try {
						completedMessages = this.completeGroup(message, correlationKey, group);
					}
					finally {
						// Always clean up even if there was an exception
						// processing messages
						this.afterRelease(group, completedMessages);
					}
				}
				else {
					Long groupTimeout = this.obtainGroupTimeout(groupProxy);
					if (groupTimeout != null && groupTimeout >= 0) {
						if (groupTimeout > 0) {
							final MessageGroup messageGroupToSchedule = groupProxy;

							scheduledFuture = this.getTaskScheduler()
									.schedule(new Runnable() {

										@Override
										public void run() {
											forceComplete(messageGroupToSchedule);
										}
									}, new Date(System.currentTimeMillis() + groupTimeout));

							if (logger.isDebugEnabled()) {
								logger.debug("Schedule MessageGroup [ " + messageGroup + "] to 'forceComplete'.");
							}
							this.expireGroupScheduledFutures.put(groupIdUuid, scheduledFuture);
						}
						else {
							this.forceComplete(groupProxy);
						}
					}
				}
			}
			else {
				getDiscardChannel().send(message);
			}
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	protected void forceComplete(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		// UUIDConverter is no-op if already converted
		Lock lock = getLockRegistry().obtain(UUIDConverter.getUUID(correlationKey).toString());
		boolean removeGroup = true;
		try {
			lock.lockInterruptibly();
			try {
				ScheduledFuture<?> scheduledFuture =
						this.expireGroupScheduledFutures.remove(UUIDConverter.getUUID(correlationKey));
				if (scheduledFuture != null) {
					boolean canceled = scheduledFuture.cancel(false);
					if (canceled && logger.isDebugEnabled()) {
						logger.debug("Cancel 'forceComplete' scheduling for MessageGroup [ " + group + "].");
					}
				}
				MessageGroup groupNow = group;
				/*
				 * If the group argument is not already complete,
				 * re-fetch it because it might have changed while we were waiting on
				 * its lock. If the last modified timestamp changed, defer the completion
				 * because the selection condition may have changed such that the group
				 * would no longer be eligible. If the timestamp changed, it's a completely new
				 * group and should not be reaped on this cycle.
				 *
				 * If the group argument is already complete, do not re-fetch.
				 * Note: not all message stores provide a direct reference to its internal
				 * group so the initial 'isComplete()` will only return true for those stores if
				 * the group was already complete at the time of its selection as a candidate.
				 *
				 * If the group is marked complete, only consider it
				 * for reaping if it's empty (and both timestamps are unaltered).
				 */
				if (!group.isComplete()) {
					groupNow = new MessageGroupProxy(this.messageStore.getGroupMetadata(correlationKey),
							this.messageStore);
				}
				long lastModifiedNow = groupNow.getLastModified();
				int groupSize = groupNow.size();
				if ((!groupNow.isComplete() || groupSize == 0)
						&& group.getLastModified() == lastModifiedNow
						&& group.getTimestamp() == groupNow.getTimestamp()) {
					if (groupSize > 0) {
						if (getReleaseStrategy().canRelease(groupNow)) {
							this.completeGroup(correlationKey, groupNow);
						}
						else {
							this.expireGroup(correlationKey, groupNow);
						}
					}
					else {
						/*
						 * By default empty groups are removed on the same schedule as non-empty
						 * groups. A longer timeout for empty groups can be enabled by
						 * setting minimumTimeoutForEmptyGroups.
						 */
						removeGroup = lastModifiedNow <= (System.currentTimeMillis() - getMinimumTimeoutForEmptyGroups());
						if (removeGroup && logger.isDebugEnabled()) {
							logger.debug("Removing empty group: " + correlationKey);
						}
					}
				}
				else {
					removeGroup = false;
					if (logger.isDebugEnabled()) {
						logger.debug("Group expiry candidate (" + correlationKey +
								") has changed - it may be reconsidered for a future expiration");
					}
				}
			}
			finally {
				if (removeGroup) {
					this.remove(group);
				}
				lock.unlock();
			}
		}
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			logger.debug("Thread was interrupted while trying to obtain lock");
		}
	}

	private static class MessageGroupProxy implements MessageGroup, Collection<Message<?>> {

		private final MessageGroupMetadata metadata;

		private final MessageGroupStore store;

		private final Object oneMonitor = new Object();

		private final Object messagesMonitor = new Object();

		private volatile Message<?> one;

		private volatile Collection<Message<?>> messages;

		MessageGroupProxy(MessageGroupMetadata metadata, MessageGroupStore store) {
			this.metadata = metadata;
			this.store = store;
		}

		@Override
		public boolean canAdd(Message<?> message) {
			return true;
		}

		@Override
		public Collection<Message<?>> getMessages() {
			if (this.messages == null) {
				synchronized (this.messagesMonitor) {
					if (this.messages == null) {
						this.messages = this.store.getMessageGroup(this.metadata.getGroupId()).getMessages();
					}
				}
			}

			return this.messages;
		}

		@Override
		public Iterator<Message<?>> iterator() {
			return getMessages().iterator();
		}

		@Override
		public Object getGroupId() {
			return this.metadata.getGroupId();
		}

		@Override
		public int getLastReleasedMessageSequenceNumber() {
			return this.metadata.getLastReleasedMessageSequenceNumber();
		}

		@Override
		public boolean isComplete() {
			return this.metadata.isComplete();
		}

		@Override
		public void complete() {
			 this.store.completeGroup(this.metadata.getGroupId());
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
			return this.metadata.size();
		}

		@Override
		public Message<?> getOne() {
			if (this.one == null) {
				synchronized (this.oneMonitor) {
					if (this.one == null) {
						this.one = this.store.getOneMessageFromGroup(this.metadata.getGroupId());
					}
				}
			}
			return this.one;
		}

		@Override
		public long getTimestamp() {
			return this.metadata.getTimestamp();
		}

		@Override
		public long getLastModified() {
			return this.metadata.getLastModified();
		}

		@Override
		public boolean isEmpty() {
			return this.metadata.size() == 0;
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(Message<?> message) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Message<?>> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

	}
}
