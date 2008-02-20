/*
 * Copyright 2002-2007 the original author or authors.
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * Simple implementation of a message channel. Each {@link Message} is
 * placed in a queue whose capacity may be specified upon construction. If no
 * capacity is specified, the {@link #DEFAULT_CAPACITY} will be used.
 * 
 * @author Mark Fisher
 */
public class SimpleChannel extends AbstractMessageChannel {

	public static final int DEFAULT_CAPACITY = 100;


	private BlockingQueue<Message<?>> queue;


	/**
	 * Create a channel with the specified queue capacity and dispatcher policy.
	 */
	public SimpleChannel(int capacity, DispatcherPolicy dispatcherPolicy) {
		super((dispatcherPolicy != null) ? dispatcherPolicy : new DispatcherPolicy());
		if (capacity > 0) {
			this.queue = new LinkedBlockingQueue<Message<?>>(capacity);
		}
		else {
			this.queue = new SynchronousQueue<Message<?>>(true);
		}
	}

	/**
	 * Create a channel with the specified queue capacity.
	 */
	public SimpleChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with the default queue capacity and the specified dispatcher policy.
	 */
	public SimpleChannel(DispatcherPolicy dispatcherPolicy) {
		this(DEFAULT_CAPACITY, dispatcherPolicy);
	}

	/**
	 * Create a channel with the default queue capacity.
	 */
	public SimpleChannel() {
		this(DEFAULT_CAPACITY, null);
	}


	protected boolean doSend(Message message, long timeout) {
		Assert.notNull(message, "'message' must not be null");
		try {
			if (timeout > 0) {
				return this.queue.offer(message, timeout, TimeUnit.MILLISECONDS);
			}
			if (timeout == 0) {
				return this.queue.offer(message);
			}
			queue.put(message);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	protected Message doReceive(long timeout) {
		try {
			if (timeout > 0) {
				return queue.poll(timeout, TimeUnit.MILLISECONDS);
			}
			if (timeout == 0) {
				return queue.poll();
			}
			return queue.take();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public List<Message<?>> clear() {
		List<Message<?>> clearedMessages = new ArrayList<Message<?>>();
		this.queue.drainTo(clearedMessages);
		return clearedMessages;
	}

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

}
