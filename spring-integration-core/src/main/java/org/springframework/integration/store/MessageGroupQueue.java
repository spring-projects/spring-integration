/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.integration.Message;

/**
 * A {@link BlockingQueue} that is backed by a {@link MessageGroupStore}. Can be used to ensure guaranteed delivery in
 * the face of transaction rollback (assuming the store is transactional) and also to ensure messages are not lost if
 * the process dies (assuming the store is durable). To use the queue across process re-starts, the same group id
 * must be provided, so it needs to be unique but identifiable with a single logical instance of the queue.
 * 
 * @author Dave Syer
 * @since 2.0
 * 
 */
public class MessageGroupQueue extends AbstractQueue<Message<?>> implements BlockingQueue<Message<?>> {

	private static final int DEFAULT_CAPACITY = -1;

	private final MessageGroupStore messageGroupStore;

	private final Object groupId;

	private final int capacity;

	// This one could be a global semaphore
	private volatile Object storeLock = new Object();

	// This one only needs to be local
	private final Object writeLock = new Object();

	// This one only needs to be local
	private final Object readLock = new Object();

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId) {
		this(messageGroupStore, groupId, DEFAULT_CAPACITY);
	}

	public MessageGroupQueue(MessageGroupStore messageGroupStore, Object groupId, int capacity) {
		this.messageGroupStore = messageGroupStore;
		this.groupId = groupId;
		this.capacity = capacity;
	}
	
	/**
	 * @param storeLock the storeLock to set
	 */
	public void setStoreLock(Object storeLock) {
		this.storeLock = storeLock;
	}

	public Iterator<Message<?>> iterator() {
		return getMessages().iterator();
	}

	public int size() {
		return this.messageGroupStore.getMessageGroup(groupId).size();
	}

	public boolean offer(Message<?> e) {
		synchronized (storeLock) {
			if (capacity>0 && messageGroupStore.getMessageGroup(groupId).size() >= capacity) {
				return false;
			}
			messageGroupStore.addMessageToGroup(groupId, e);
		}
		synchronized (readLock) {
			readLock.notifyAll();
		}
		return true;
	}

	public Message<?> peek() {
		Collection<Message<?>> messages = getMessages();
		if (messages.isEmpty()) {
			return null;
		}
		return messages.iterator().next();
	}

	public Message<?> poll() {
		Message<?> result = null;
		synchronized (storeLock) {
			result = this.messageGroupStore.pollMessageFromGroup(groupId);
		}
		synchronized (writeLock) {
			writeLock.notifyAll();
		}
		return result;
	}

	public int drainTo(Collection<? super Message<?>> c) {
		synchronized (storeLock) {
			for (Message<?> message = this.messageGroupStore.pollMessageFromGroup(groupId); message != null;) {
				c.add(message);	
			}
		}
		synchronized (writeLock) {
			writeLock.notifyAll();
		}
		return this.messageGroupStore.getMessageGroup(groupId).size();
	}

	public int drainTo(Collection<? super Message<?>> c, int maxElements) {
		ArrayList<Message<?>> list = new ArrayList<Message<?>>();
		synchronized (storeLock) {
			Message<?> message = this.messageGroupStore.pollMessageFromGroup(groupId);
			for (int i = 0; i < maxElements && message != null; i++) {
				list.add(message);	
				message = this.messageGroupStore.pollMessageFromGroup(groupId);
			}
		}
		synchronized (writeLock) {
			writeLock.notifyAll();
		}
		c.addAll(list);
		return list.size();
	}

	public boolean offer(Message<?> e, long timeout, TimeUnit unit) throws InterruptedException {
		long threshold = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
		boolean result = offer(e);
		while (!result && System.currentTimeMillis() < threshold) {
			synchronized (writeLock) {
				writeLock.wait(threshold - System.currentTimeMillis());
			}
			result = offer(e);
		}
		return result;
	}

	public Message<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
		Message<?> message = poll();
		if (message != null) {
			return message;
		}
		long threshold = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
		while (message == null && System.currentTimeMillis() < threshold) {
			synchronized (readLock) {
				readLock.wait(threshold - System.currentTimeMillis());
			}
			message = poll();
		}
		return message;
	}

	public void put(Message<?> e) throws InterruptedException {
		while (!offer(e)) {
			synchronized (writeLock) {
				writeLock.wait();
			}
		}
	}

	public int remainingCapacity() {
		return (capacity>0 ? capacity : Integer.MAX_VALUE) - messageGroupStore.getMessageGroup(groupId).size();
	}

	public Message<?> take() throws InterruptedException {
		Message<?> message = poll();
		while (message == null) {
			synchronized (readLock) {
				readLock.wait();
			}
			message = poll();
		}
		return message;
	}

	private Collection<Message<?>> getMessages(){
		return messageGroupStore.getMessageGroup(groupId).getMessages();
	}

}
