/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagePriority;

/**
 * A message channel that prioritizes messages based on a {@link Comparator}.
 * The default comparator is based upon the message header's 'priority'.
 * 
 * @author Mark Fisher
 */
public class PriorityChannel extends QueueChannel {

	private final Semaphore semaphore;


	/**
	 * Create a channel with the specified queue capacity. If the capacity
	 * is a non-positive value, the queue will be unbounded. Message priority
	 * will be determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel(int capacity, Comparator<Message<?>> comparator) {
		super(new PriorityBlockingQueue<Message<?>>(11,
				(comparator != null) ? comparator : new MessagePriorityComparator()));
		this.semaphore =  (capacity > 0) ? new Semaphore(capacity, true) : null;
	}

	/**
	 * Create a channel with the specified queue capacity. Message priority
	 * will be based upon the value of {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel(Comparator<Message<?>> comparator) {
		this(0, comparator);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * based on the value of {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel() {
		this(0, null);
	}


	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (!acquirePermitIfNecessary(timeout)) {
			return false;
		}
		return super.doSend(message, 0);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		Message<?> message = super.doReceive(timeout);
		if (message != null) {
			this.releasePermitIfNecessary();
			return message;
		}
		return null;
	}

	private boolean acquirePermitIfNecessary(long timeoutInMilliseconds) {
		if (this.semaphore != null) {
			try {
				return this.semaphore.tryAcquire(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return true;
	}

	private void releasePermitIfNecessary() {
		if (this.semaphore != null) {
			this.semaphore.release();
		}
	}


	private static class MessagePriorityComparator implements Comparator<Message<?>> {

		public int compare(Message<?> message1, Message<?> message2) {
			MessagePriority priority1 = message1.getHeaders().getPriority();
			MessagePriority priority2 = message2.getHeaders().getPriority();
			priority1 = priority1 != null ? priority1 : MessagePriority.NORMAL;
			priority2 = priority2 != null ? priority2 : MessagePriority.NORMAL;
			return priority1.compareTo(priority2);
		}
	}

}
