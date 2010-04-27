/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.integration.core.Message;

/**
 * A channel implementation that stores messages in a thread-bound queue. In
 * other words, send() will put a message at the tail of the queue for the
 * current thread, and receive() will retrieve a message from the head of the
 * queue. Since, by definition, only one thread will interact with the queue
 * at a time, the timeout values on send and receive have no effect. If there
 * are no Messages in the queue, the receive operations will return a
 * <code>null</code> value immediately, regardless of any timeout value.
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
	 * The thread-bound Queue.
	 */
	private static class ThreadLocalMessageHolder extends ThreadLocal<Queue<Message<?>>> {

		@Override
		protected Queue<Message<?>> initialValue() {
			return new LinkedBlockingQueue<Message<?>>();
		}
	}

}
