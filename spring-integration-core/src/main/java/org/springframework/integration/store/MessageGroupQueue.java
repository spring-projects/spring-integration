/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
 *
 * @since 2.0
 *
 */
public class MessageGroupQueue extends AbstractQueue<Message<?>> implements BlockingQueue<Message<?>> {

	private final Log logger = LogFactory.getLog(getClass());

	private static final int DEFAULT_CAPACITY = Integer.MAX_VALUE;

	private final MessageGroupStore messageGroupStore;

	private final Object groupId;

	private final int capacity;

	//This one could be a global semaphore
	private final Lock storeLock;

	private final Condition messageStoreNotFull;

	private final Condition messageStoreNotEmpty;

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId) {
		this(messageGroupStore, groupId, DEFAULT_CAPACITY, new ReentrantLock(true));
	}

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, int capacity) {
		this(messageGroupStore, groupId, capacity, new ReentrantLock(true));
	}

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, Lock storeLock) {
		this(messageGroupStore, groupId, DEFAULT_CAPACITY, storeLock);
	}

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, int capacity, Lock storeLock) {
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
	}

	public Iterator<Message<?>> iterator() {
		return getMessages().iterator();
	}

	public int size() {
		return messageGroupStore.messageGroupSize(groupId);
	}

	public Message<?> peek() {
		Message<?> message = null;
		final Lock storeLock = this.storeLock;
		try {
			storeLock.lockInterruptibly();
			try {
				Collection<Message<?>> messages = getMessages();
				if (!messages.isEmpty()) {
					message = messages.iterator().next();
				}
			}
			finally {
				storeLock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return message;
	}

	public Message<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
		Message<?> message = null;
		long timeoutInNanos = unit.toNanos(timeout);
		final Lock storeLock = this.storeLock;
		storeLock.lockInterruptibly();

		try {
			while (this.size() == 0 && timeoutInNanos > 0){
				timeoutInNanos = this.messageStoreNotEmpty.awaitNanos(timeoutInNanos);
			}
			message = this.doPoll();

		}
		finally {
			storeLock.unlock();
		}
		return message;
	}

	public Message<?> poll() {
		Message<?> message = null;
		final Lock storeLock = this.storeLock;
		try {
			storeLock.lockInterruptibly();
			try {
				message = this.doPoll();
			}
			finally {
				storeLock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return message;
	}

	public int drainTo(Collection<? super Message<?>> c) {
		return this.drainTo(c, Integer.MAX_VALUE);
	}

	public int drainTo(Collection<? super Message<?>> collection, int maxElements) {
		Assert.notNull(collection, "'collection' must not be null");
		int originalSize = collection.size();
		ArrayList<Message<?>> list = new ArrayList<Message<?>>();
		final Lock storeLock = this.storeLock;
		try {
			storeLock.lockInterruptibly();
			try {
				Message<?> message = this.messageGroupStore.pollMessageFromGroup(groupId);
				for (int i = 0; i < maxElements && message != null; i++) {
					list.add(message);
					message = this.messageGroupStore.pollMessageFromGroup(groupId);
				}
				this.messageStoreNotFull.signal();
			}
			finally {
				storeLock.unlock();
			}
		}
		catch (InterruptedException e) {
			logger.warn("Queue may not have drained completely since this operation was interrupted", e);
			Thread.currentThread().interrupt();
		}
		collection.addAll(list);
		return collection.size() - originalSize;
	}

	public boolean offer(Message<?> message) {
		boolean offered = true;
		final Lock storeLock = this.storeLock;
		try {
			storeLock.lockInterruptibly();
			try {
				offered = this.doOffer(message);
			}
			finally {
				storeLock.unlock();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return offered;
	}

	public boolean offer(Message<?> message, long timeout, TimeUnit unit) throws InterruptedException {
		long timeoutInNanos = unit.toNanos(timeout);
		boolean offered = false;

		final Lock storeLock = this.storeLock;
		storeLock.lockInterruptibly();
		try {
			if (capacity != Integer.MAX_VALUE) {
				while (this.size() == capacity && timeoutInNanos > 0){
					timeoutInNanos = this.messageStoreNotFull.awaitNanos(timeoutInNanos);
				}
			}
			if (timeoutInNanos > 0){
				offered = this.doOffer(message);
			}
		}
		finally {
			storeLock.unlock();
		}
		return offered;
	}

	public void put(Message<?> message) throws InterruptedException {
		final Lock storeLock = this.storeLock;
		storeLock.lockInterruptibly();
		try {
			if (capacity != Integer.MAX_VALUE) {
				while (this.size() == capacity){
					this.messageStoreNotFull.await();
				}
			}
			this.doOffer(message);
		}
		finally {
			storeLock.unlock();
		}
	}

	public int remainingCapacity() {
		if (capacity == Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return capacity - this.size();
	}

	public Message<?> take() throws InterruptedException {
		Message<?> message = null;
		final Lock storeLock = this.storeLock;
		storeLock.lockInterruptibly();

		try {
			while (this.size() == 0){
				this.messageStoreNotEmpty.await();
			}
			message = this.doPoll();

		}
		finally {
			storeLock.unlock();
		}
		return message;
	}

	private Collection<Message<?>> getMessages(){
		return messageGroupStore.getMessageGroup(groupId).getMessages();
	}

	/**
	 * It is assumed that the 'storeLock' is being held by the caller, otherwise
	 * IllegalMonitorStateException may be thrown
	 */
	private Message<?> doPoll() {
		Message<?> message = this.messageGroupStore.pollMessageFromGroup(groupId);
		this.messageStoreNotFull.signal();
		return message;
	}

	/**
	 * It is assumed that the 'storeLock' is being held by the caller, otherwise
	 * IllegalMonitorStateException may be thrown
	 */
	private boolean doOffer(Message<?> message){
		boolean offered = false;
		if (capacity == Integer.MAX_VALUE || this.size() < capacity){
			messageGroupStore.addMessageToGroup(groupId, message);
			offered = true;
			this.messageStoreNotEmpty.signal();
		}
		return offered;
	}
}
