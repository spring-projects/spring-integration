/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Simple implementation of a message channel. Each {@link Message} is placed in
 * a {@link BlockingQueue} whose capacity may be specified upon construction.
 * The capacity must be a positive integer value. For a zero-capacity version
 * based upon a {@link java.util.concurrent.SynchronousQueue}, consider the
 * {@link RendezvousChannel}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class QueueChannel extends AbstractPollableChannel implements QueueChannelOperations {

	private final Queue<Message<?>> queue;

	protected final ReentrantLock queueLock = new ReentrantLock();

	protected final Condition queueNotEmpty = this.queueLock.newCondition();

	/**
	 * Create a channel with the specified queue.
	 *
	 * @param queue The queue.
	 */
	public QueueChannel(Queue<Message<?>> queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

	/**
	 * Create a channel with the specified queue capacity.
	 *
	 * @param capacity The capacity.
	 */
	public QueueChannel(int capacity) {
		Assert.isTrue(capacity > 0, "The capacity must be a positive integer. " +
				"For a zero-capacity alternative, consider using a 'RendezvousChannel'.");
		this.queue = new LinkedBlockingQueue<Message<?>>(capacity);
	}

	/**
	 * Create a channel with "unbounded" queue capacity. The actual capacity value is
	 * {@link Integer#MAX_VALUE}. Note that a bounded queue is recommended, since an
	 * unbounded queue may lead to OutOfMemoryErrors.
	 */
	public QueueChannel() {
		this(new LinkedBlockingQueue<Message<?>>());
	}


	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.notNull(message, "'message' must not be null");
		try {
			if (this.queue instanceof BlockingQueue) {
				BlockingQueue<Message<?>> blockingQueue = (BlockingQueue<Message<?>>) this.queue;
				if (timeout > 0) {
					return blockingQueue.offer(message, timeout, TimeUnit.MILLISECONDS);
				}
				if (timeout == 0) {
					return blockingQueue.offer(message);
				}
				blockingQueue.put(message);
				return true;
			}
			else {
				this.queueLock.lockInterruptibly();
				try {
					return this.queue.offer(message);
				}
				finally {
					this.queueNotEmpty.signalAll();
					this.queueLock.unlock();
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		try {
			if (timeout > 0) {
				if (this.queue instanceof BlockingQueue) {
					return ((BlockingQueue<Message<?>>) this.queue).poll(timeout, TimeUnit.MILLISECONDS);
				}
				else {
					long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
					this.queueLock.lockInterruptibly();
					try {
						while (this.queue.size() == 0 && nanos > 0) {
							nanos = this.queueNotEmpty.awaitNanos(nanos);
						}
						return this.queue.poll();
					}
					finally {
						this.queueLock.unlock();
					}
				}
			}
			if (timeout == 0) {
				return this.queue.poll();
			}

			if (this.queue instanceof BlockingQueue) {
				return ((BlockingQueue<Message<?>>) this.queue).take();
			}
			else {
				this.queueLock.lockInterruptibly();
				try {
					while (this.queue.size() == 0) {
						this.queueNotEmpty.await();
					}
					return this.queue.poll();
				}
				finally {
					this.queueLock.unlock();
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Override
	public List<Message<?>> clear() {
		List<Message<?>> clearedMessages = new ArrayList<Message<?>>();
		if (this.queue instanceof BlockingQueue) {
			((BlockingQueue<Message<?>>) this.queue).drainTo(clearedMessages);
		}
		else {
			Message<?> message = null;
			while ((message = this.queue.poll()) != null) {
				clearedMessages.add(message);
			}
		}
		return clearedMessages;
	}

	@Override
	public List<Message<?>> purge(MessageSelector selector) {
		if (selector == null) {
			return this.clear();
		}
		List<Message<?>> purgedMessages = new ArrayList<Message<?>>();
		Object[] array = this.queue.toArray();
		for (Object o : array) {
			Message<?> message = (Message<?>) o;
			if (!selector.accept(message) && this.queue.remove(message)) {
				purgedMessages.add(message);
			}
		}
		return purgedMessages;
	}

	@Override
	public int getQueueSize() {
		return this.queue.size();
	}

	@Override
	public int getRemainingCapacity() {
		if (this.queue instanceof BlockingQueue) {
			return ((BlockingQueue<Message<?>>) this.queue).remainingCapacity();
		}
		else {
			//Assume that underlying Queue implementation takes care of its size on "offer".
			return Integer.MAX_VALUE;
		}
	}

}
