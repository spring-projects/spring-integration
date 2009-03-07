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

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Round-robin implementation of {@link MessageDispatcher} that will attempt to
 * send a {@link Message} to one of its handlers. As soon as <em>one</em> of the
 * handlers accepts the Message, the dispatcher will return 'true'. This
 * implementation will load balance over its handlers using a round-robin
 * strategy.
 * <p>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers reject the Message, the dispatcher will throw a
 * MessageRejectedException.
 * 
 * @author Iwein Fuld
 */
public class LoadBalancingDispatcher extends AbstractSendOnceDispatcher {

	private ReentrantLock queueLock = new ReentrantLock();
	
	public boolean dispatch(Message<?> message) {
		queueLock.lock();
		Queue<MessageHandler> handlers = this.getHandlers();
		if (handlers.isEmpty()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		boolean success = false;
		int size = handlers.size();
		queueLock.unlock();
		for (int i = 0; i < size && success == false; i++) {
			queueLock.lock();
			MessageHandler handler = handlers.poll();
			handlers.offer(handler);
			queueLock.unlock();
			if (this.sendMessageToHandler(message, handler)) {
				success = true;
			}
		}
		if (!success) {
			throw new MessageRejectedException(message, "All of dispatcher's subscribers rejected Message.");
		}
		return success;
	}
}
