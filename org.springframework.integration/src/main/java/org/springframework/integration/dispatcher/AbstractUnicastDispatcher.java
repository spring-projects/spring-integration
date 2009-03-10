/* Copyright 2002-2008 the original author or authors.
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

import java.util.Iterator;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Implementation of {@link MessageDispatcher} that will attempt to send a
 * {@link Message} to at most one of its handlers. The handlers will be tried
 * one by one. As soon as <em>one</em> of the handlers accepts the Message, the
 * dispatcher will return <code>true</code> and ignore the rest of it's
 * handlers.
 * <p/>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers reject the Message, the dispatcher will throw a
 * MessageRejectedException.
 * <p/>
 * The implementations of this class control the order in which the handlers
 * will be tried through the implementation of the {@link #getHandlerIterator(List)} method.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * 
 */
public abstract class AbstractUnicastDispatcher extends AbstractDispatcher {

	public final boolean dispatch(Message<?> message) {
		boolean success = false;
		List<MessageHandler> handlers = this.getHandlers();
		if (handlers.isEmpty()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		Iterator<MessageHandler> handlerIterator = getHandlerIterator(handlers);
		while (success == false && handlerIterator.hasNext()) {
			MessageHandler handler = handlerIterator.next();
			if (this.sendMessageToHandler(message, handler)) {
				success = true; // we have a winner.
			}
		}
		if (!success) {
			throw new MessageRejectedException(message, "All of dispatcher's subscribers rejected Message.");
		}
		return success;
	}

	/**
	 * Return the iterator that will be used to loop over the handlers. This
	 * allows subclasses to control the order of iteration for each
	 * {@link #dispatch(Message)} invocation.
	 * @param handlers
	 * @return
	 */
	protected abstract Iterator<MessageHandler> getHandlerIterator(List<MessageHandler> handlers);
}
