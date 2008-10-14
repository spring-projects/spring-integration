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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.integration.message.Message;
import org.springframework.integration.selector.MessageSelector;

/**
 * A channel implementation that stores messages in a thread-bound queue. In
 * other words, send() will put a message at the tail of the queue for the
 * current thread, and receive() will retrieve a message from the head of the
 * queue.
 * 
 * @author Dave Syer
 * @author Mark Fisher
 */
public class ThreadLocalChannel extends AbstractPollableChannel {

	private static final ThreadLocalMessageHolder messageHolder = new ThreadLocalMessageHolder();


	@Override
	protected Message<?> doReceive(long timeout) {
		return messageHolder.get().poll();
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (message == null) {
			return false;
		}
		return messageHolder.get().add(message);
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


	private static class ThreadLocalMessageHolder extends ThreadLocal<Queue<Message<?>>> {

		@Override
		protected Queue<Message<?>> initialValue() {
			return new LinkedBlockingQueue<Message<?>>();
		}
	}

}
