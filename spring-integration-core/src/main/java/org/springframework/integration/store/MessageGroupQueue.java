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

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link BlockingQueue} that is backed by a {@link MessageGroupStore}. Can be used to ensure guaranteed delivery in
 * the face of transaction rollback (assuming the store is transactional) and also to ensure messages are not lost if
 * the process dies (assuming the store is durable). To use the queue across process re-starts, the same group id
 * must be provided, so it needs to be unique but identifiable with a single logical instance of the queue.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class MessageGroupQueue extends AbstractQueue<Message<?>> implements BlockingQueue<Message<?>> {

	private final Log logger = LogFactory.getLog(getClass());

	private static final int DEFAULT_CAPACITY = Integer.MAX_VALUE;

	private final BasicMessageGroupStore messageGroupStore;

	private final Object groupId;

	private final int capacity;

	//This one could be a global semaphore
	private final Lock storeLock;

	private final Condition messageStoreNotFull;

	private final Condition messageStoreNotEmpty;

	public MessageGroupQueue(BasicMessageGroupStore messageGroupStore, Object groupId) {
		this(messageGroupStore, groupId, DEFAULT_CAPACITY, new ReentrantLock(true));
	}

	public MessageGroupQueue(BasicMessageGroupStore messageGroupStore, Object groupId, int capacity) {
		this(messageGroupStore, groupId, capacity, new ReentrantLock(true));
	}

	public MessageGroupQueue(BasicMessageGroupStore messageGroupStore, Object groupId, Lock storeLock) {
		this(messageGroupStore, groupId, DEFAULT_CAPACITY, storeLock);
	}

	public MessageGroupQueue(BasicMessageGroupStore messageGroupStore, Object groupId, int capacity, Lock storeLock) {
		Assert.isTrue(capacity > 0, "'capacity' must be greater than 0");
		Assert.notNull(storeLock, "'storeLock' must not be null");
		Assert.notNull(messageGroupStore, "'messageGroupStore' must not be null");
		Assert.notNull(groupId, "'groupId' must not be null");
		this.storeLock = storeLock;
		this.messageStoreNotFull = this.storeLock.newCondition();
		this.messageStoreNotEmpty = this.storeLock.newCondition();
		this.messageGroupStore = messageGroupStore;
		this.groupId = groupId;
		this.capacity = capacity;
		if (this.logger.isWarnEnabled() && !(messageGroupStore instanceof ChannelMessageStore)) {
			this.logger.warn(messageGroupStore.getClass().getSimpleName() + " is not optimized for use "
					+ "in a 'MessageGroupQueue'; consider using a `ChannelMessageStore'");
		}
	}

	/**
	 * If true, ensures that the message store supports priority. If false WARNs if the
	 * message store uses priority to determine the message order when receiving.
	 * @param priority true if priority is expected to be used.
	 */
	public void setPriority(boolean priority) {
		if (priority) {
			Assert.isInstanceOf(PriorityCapableChannelMessageStore.class, this.messageGroupStore);
			Assert.isTrue(((PriorityCapableChannelMessageStore) this.messageGroupStore).isPriorityEnabled(),
					"When using priority, the 'PriorityCapableChannelMessageStore' must have priority enabled.");
		}
		else {
			if (this.logger.isWarnEnabled() && this.messageGroupStore instanceof PriorityCapableChannelMessageStore
					&& ((PriorityCapableChannelMessageStore) this.messageGroupStore).isPriorityEnabled()) {
				this.logger.warn("It's not recommended to use a priority-based message store " +
						"when declaring a non-priority 'MessageGroupQueue'; message retrieval may not be FIFO; " +
						"set 'priority' to 'true' if that is your intent. If you are using the namespace to " +
						"define a channel, use '<priority-queue message-store.../> instead.");
			}
		}
	}

	@Override
	public Iterator<Message<?>> iterator() {
		return stream().iterator();
	}

	/**
	 * Get the store.
	 * @return the store.
	 * @since 5.0.11
	 */
	protected BasicMessageGroupStore getMessageGroupStore() {
		return this.messageGroupStore;
	}

	/**
	 * Get the store lock.
	 * @return the lock.
	 * @since 5.0.11
	 */
	protected Lock getStoreLock() {
		return this.storeLock;
	}

	/**
	 * Get the not full condition.
	 * @return the condition.
	 * @since 5.0.11
	 */
	protected Condition getMessageStoreNotFull() {
		return this.messageStoreNotFull;
	}

	/**
	 * Get the not empty condition.
	 * @return the condition.
	 * @since 5.0.11
	 */
	protected Condition getMessageStoreNotEmpty() {
		return this.messageStoreNotEmpty;
	}

	@Override
	public int size() {
		return this.messageGroupStore.messageGroupSize(this.groupId);
	}

	@Override
	public Message<?> peek() {
		try {
			this.storeLock.lockInterruptibly();
			try (Stream<Message<?>> messageStream = stream()) {
				return messageStream.findFirst().orElse(null); // NOSONAR
			}
			finally {
				this.storeLock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	@Override
	public Message<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
		Message<?> message;
		long timeoutInNanos = unit.toNanos(timeout);
		final Lock lock = this.storeLock;
		lock.lockInterruptibly();

		try {
			message = doPoll();
			while (message == null && timeoutInNanos > 0) {
				timeoutInNanos = this.messageStoreNotEmpty.awaitNanos(timeoutInNanos);
				message = doPoll();
			}
		}
		finally {
			lock.unlock();
		}
		return message;
	}

	@Override
	public Message<?> poll() {
		Message<?> message = null;
		final Lock lock = this.storeLock;
		try {
			lock.lockInterruptibly();
			try {
				message = this.doPoll();
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return message;
	}

	@Override
	public int drainTo(Collection<? super Message<?>> c) {
		return this.drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(Collection<? super Message<?>> collection, int maxElements) {
		Assert.notNull(collection, "'collection' must not be null");
		int originalSize = collection.size();
		ArrayList<Message<?>> list = new ArrayList<>();
		final Lock lock = this.storeLock;
		try {
			lock.lockInterruptibly();
			try {
				Message<?> message = this.messageGroupStore.pollMessageFromGroup(this.groupId);
				for (int i = 0; i < maxElements && message != null; i++) {
					list.add(message);
					message = this.messageGroupStore.pollMessageFromGroup(this.groupId);
				}
				this.messageStoreNotFull.signal();
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			this.logger.warn("Queue may not have drained completely since this operation was interrupted", e);
			Thread.currentThread().interrupt();
		}
		collection.addAll(list);
		return collection.size() - originalSize;
	}

	@Override
	public boolean offer(Message<?> message) {
		boolean offered = true;
		final Lock lock = this.storeLock;
		try {
			lock.lockInterruptibly();
			try {
				offered = this.doOffer(message);
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return offered;
	}

	@Override
	public boolean offer(Message<?> message, long timeout, TimeUnit unit) throws InterruptedException {
		long timeoutInNanos = unit.toNanos(timeout);
		boolean offered = false;

		final Lock lock = this.storeLock;
		lock.lockInterruptibly();
		try {
			if (this.capacity != Integer.MAX_VALUE) {
				while (this.size() == this.capacity && timeoutInNanos > 0) {
					timeoutInNanos = this.messageStoreNotFull.awaitNanos(timeoutInNanos);
				}
			}
			if (timeoutInNanos > 0) {
				offered = this.doOffer(message);
			}
		}
		finally {
			lock.unlock();
		}
		return offered;
	}

	@Override
	public void put(Message<?> message) throws InterruptedException {
		final Lock lock = this.storeLock;
		lock.lockInterruptibly();
		try {
			if (this.capacity != Integer.MAX_VALUE) {
				while (this.size() == this.capacity) {
					this.messageStoreNotFull.await();
				}
			}
			this.doOffer(message);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public int remainingCapacity() {
		if (this.capacity == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return this.capacity - this.size();
	}

	@Override
	public Message<?> take() throws InterruptedException {
		Message<?> message;
		final Lock lock = this.storeLock;
		lock.lockInterruptibly();

		try {
			while (this.size() == 0) {
				this.messageStoreNotEmpty.await();
			}
			message = doPoll();

		}
		finally {
			lock.unlock();
		}
		return message;
	}

	protected Collection<Message<?>> getMessages() {
		return this.messageGroupStore.getMessageGroup(this.groupId).getMessages();
	}

	@Override
	public Stream<Message<?>> stream() {
		return this.messageGroupStore.getMessageGroup(this.groupId).streamMessages();
	}

	/**
	 * It is assumed that the 'storeLock' is being held by the caller, otherwise
	 * IllegalMonitorStateException may be thrown
	 * @return a message // TODO @Nullable
	 */
	protected Message<?> doPoll() {
		Message<?> message = this.messageGroupStore.pollMessageFromGroup(this.groupId);
		this.messageStoreNotFull.signal();
		return message;
	}

	/**
	 * It is assumed that the 'storeLock' is being held by the caller, otherwise
	 * IllegalMonitorStateException may be thrown
	 * @param message the message to offer.
	 * @return true if offered.
	 */
	protected boolean doOffer(Message<?> message) {
		boolean offered = false;
		if (this.capacity == Integer.MAX_VALUE || this.size() < this.capacity) {
			this.messageGroupStore.addMessageToGroup(this.groupId, message);
			offered = true;
			this.messageStoreNotEmpty.signal();
		}
		return offered;
	}

}
