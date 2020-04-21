/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.core.MessageSelector;
import org.springframework.lang.Nullable;
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
@SuppressWarnings("deprecation")
public class QueueChannel extends AbstractPollableChannel implements QueueChannelOperations,
		org.springframework.integration.support.management.QueueChannelManagement {

	private final Queue<Message<?>> queue;

	protected final Semaphore queueSemaphore = new Semaphore(0); // NOSONAR final

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
		this.queue = new LinkedBlockingQueue<>(capacity);
	}

	/**
	 * Create a channel with "unbounded" queue capacity. The actual capacity value is
	 * {@link Integer#MAX_VALUE}. Note that a bounded queue is recommended, since an
	 * unbounded queue may lead to OutOfMemoryErrors.
	 */
	public QueueChannel() {
		this(new LinkedBlockingQueue<>());
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
				try {
					return this.queue.offer(message);
				}
				finally {
					this.queueSemaphore.release();
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	@Override
	@Nullable
	protected Message<?> doReceive(long timeout) {
		try {
			if (timeout > 0) {
				if (this.queue instanceof BlockingQueue) {
					return ((BlockingQueue<Message<?>>) this.queue).poll(timeout, TimeUnit.MILLISECONDS);
				}
				else {
					return pollNonBlockingQueue(timeout);
				}
			}
			if (timeout == 0) {
				return this.queue.poll();
			}

			if (this.queue instanceof BlockingQueue) {
				return ((BlockingQueue<Message<?>>) this.queue).take();
			}
			else {
				Message<?> message = this.queue.poll();
				while (message == null) {
					this.queueSemaphore.tryAcquire(50, TimeUnit.MILLISECONDS); // NOSONAR ok to ignore result
					message = this.queue.poll();
				}
				return message;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@Nullable
	private Message<?> pollNonBlockingQueue(long timeout) throws InterruptedException {
		Message<?> message = this.queue.poll();
		if (message == null) {
			long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
			long deadline = System.nanoTime() + nanos;
			while (message == null && nanos > 0) {
				this.queueSemaphore.tryAcquire(nanos, TimeUnit.NANOSECONDS); // NOSONAR ok to ignore result
				message = this.queue.poll();
				if (message == null) {
					nanos = deadline - System.nanoTime();
				}
			}
		}
		return message;
	}

	@Override
	public List<Message<?>> clear() {
		List<Message<?>> clearedMessages = new ArrayList<>();
		if (this.queue instanceof BlockingQueue) {
			((BlockingQueue<Message<?>>) this.queue).drainTo(clearedMessages);
		}
		else {
			Message<?> message;
			while ((message = this.queue.poll()) != null) {
				clearedMessages.add(message);
			}
		}
		return clearedMessages;
	}

	@Override
	public List<Message<?>> purge(@Nullable MessageSelector selector) {
		if (selector == null) {
			return this.clear();
		}
		List<Message<?>> purgedMessages = new ArrayList<>();
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
