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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.MessagePriority;

/**
 * A message channel that prioritizes messages based on a {@link Comparator}.
 * The default comparator is based upon the message header's 'priority'.
 * 
 * @author Mark Fisher
 */
public class PriorityChannel extends QueueChannel {

	private final Semaphore semaphore;


	/**
	 * Create a channel with the specified queue capacity.
	 * Priority will be based upon the provided {@link Comparator}.
	 */
	public PriorityChannel(int capacity, Comparator<Message<?>> comparator) {
		super(new PriorityBlockingQueue<Message<?>>(capacity, comparator));
		this.semaphore = new Semaphore(capacity, true);
	}

	/**
	 * Create a channel with the specified queue capacity.
	 * Priority will be based upon the value of {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel(int capacity) {
		this(capacity, new MessagePriorityComparator());
	}

	/**
	 * Create a channel with the default queue capacity and dispatcher policy.
	 * Priority will be based on the value of {@link MessageHeader#getPriority()}.
	 */
	public PriorityChannel() {
		this(DEFAULT_CAPACITY, new MessagePriorityComparator());
	}


	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			if (!this.semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
				return false;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return super.doSend(message, 0);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		Message<?> message = super.doReceive(timeout);
		if (message != null) {
			this.semaphore.release();
			return message;
		}
		return null;
	}


	private static class MessagePriorityComparator implements Comparator<Message<?>> {

		public int compare(Message<?> message1, Message<?> message2) {
			MessagePriority priority1 = message1.getHeader().getPriority();
			MessagePriority priority2 = message2.getHeader().getPriority();
			return priority1.compareTo(priority2);
		}
	}

}
