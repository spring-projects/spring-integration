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

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.SubscribableSource;
import org.springframework.integration.message.Target;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * A channel that invokes the subscribed {@link MessageHandler handler(s)} in a
 * sender's thread (returning after at most one handles the message). If a
 * {@link PollableSource} is provided, then that source will likewise be polled
 * within a receiver's thread.
 * <p>
 * If the channel has no subscribed handlers and no configured source, then it
 * will store messages in a thread-bound queue. In other words, send() will put
 * a message at the tail of the queue for the current thread, and receive() will
 * retrieve a message from the head of the queue.
 * 
 * @author Dave Syer
 * @author Mark Fisher
 */
public class SynchronousChannel extends AbstractMessageChannel implements SubscribableSource {

	private static final ThreadLocalMessageHolder messageHolder = new ThreadLocalMessageHolder();


	private volatile PollableSource<?> source;

	private final SimpleDispatcher dispatcher;

	private final AtomicInteger handlerCount = new AtomicInteger();


	public SynchronousChannel() {
		this(null);
	}

	public SynchronousChannel(PollableSource<?> source) {
		super(defaultDispatcherPolicy());
		this.source = source;
		this.dispatcher = new SimpleDispatcher(this.getDispatcherPolicy());
	}


	public void setSource(PollableSource<?> source) {
		this.source = source;
	}

	public boolean subscribe(Target target) {
		boolean added = this.dispatcher.subscribe(target);
		if (added) {
			this.handlerCount.incrementAndGet();
		}
		return added;
	}

	public boolean unsubscribe(Target target) {
		boolean removed = this.dispatcher.unsubscribe(target);
		if (removed) {
			this.handlerCount.decrementAndGet();
		}
		return removed;
	}


	@Override
	protected Message<?> doReceive(long timeout) {
		if (this.source != null) {
			Message<?> result = this.source.receive();
			if (result != null) {
				return result;
			}
		}
		return messageHolder.get().poll();
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (message == null) {
			return false;
		}
		if (this.handlerCount.get() > 0) {
			return this.dispatcher.dispatch(message);
		}
		else if (this.source == null) {
			return messageHolder.get().add(message);
		}
		return false;
	}

	/**
	 * Remove and return any messages that are stored for the current thread.
	 */
	public List<Message<?>> clear() {
		List<Message<?>> removedMessages = new ArrayList<Message<?>>();
		Message<?> next = messageHolder.get().poll();
		while (next != null) {
			removedMessages.add(next);
			next = messageHolder.get().poll();
		}
		return removedMessages;
	}

	/**
	 * Remove and return any messages that are stored for the current thread
	 * and do not match the provided selector.
	 */
	public List<Message<?>> purge(MessageSelector selector) {
		List<Message<?>> removedMessages = new ArrayList<Message<?>>();
		Object[] allMessages = messageHolder.get().toArray();
		for (Object next : allMessages) {
			Message<?> message = (Message<?>) next;
			if (!selector.accept(message) && messageHolder.get().remove(message)) {
				removedMessages.add(message);
			}
		}
		return removedMessages;
	}


	private static DispatcherPolicy defaultDispatcherPolicy() {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(false);
		dispatcherPolicy.setMaxMessagesPerTask(1);
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setRejectionLimit(1);
		dispatcherPolicy.setRetryInterval(0);
		dispatcherPolicy.setShouldFailOnRejectionLimit(false);
		return dispatcherPolicy;
	}


	private static class ThreadLocalMessageHolder extends ThreadLocal<Queue<Message<?>>> {

		@Override
		protected Queue<Message<?>> initialValue() {
			return new LinkedBlockingQueue<Message<?>>();
		}
	}

}
