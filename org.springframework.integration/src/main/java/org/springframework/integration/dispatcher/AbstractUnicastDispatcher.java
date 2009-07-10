/* Copyright 2002-2009 the original author or authors.
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
 * dispatcher will return <code>true</code> and ignore the rest of its handlers.
 * <p/>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers reject the Message, the dispatcher will throw a
 * {@link MessageRejectedException}.
 * <p/>
 * The implementations of this class control the order in which the handlers
 * will be tried through the implementation of the {@link #getHandlerIterator(List)} method.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 1.0.2
 */
public abstract class AbstractUnicastDispatcher extends AbstractDispatcher {

	public final boolean dispatch(Message<?> message) {
		boolean success = false;
		Iterator<MessageHandler> handlerIterator = this.getHandlerIterator();
		if (!handlerIterator.hasNext()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		List<RuntimeException> exceptions = new ArrayList<RuntimeException>();
		while (success == false && handlerIterator.hasNext()) {
			MessageHandler handler = handlerIterator.next();
			try {
				handler.handleMessage(message);
				success = true; // we have a winner.
			}
			catch (Exception e) {
				RuntimeException runtimeException = (e instanceof RuntimeException)
						? (RuntimeException) e
						: new MessageDeliveryException(message,
								"Dispatcher failed to deliver Message.", e);
				exceptions.add(runtimeException);
				this.handleExceptions(exceptions, message, !handlerIterator.hasNext());
			}
		}
		return success;
	}

	/**
	 * Return the iterator that will be used to loop over the handlers. This
	 * default simply returns the Iterator for the existing handler List. This
	 * method can be overridden by subclasses to control the order of iteration
	 * for each {@link #dispatch(Message)} invocation.
	 */
	protected Iterator<MessageHandler> getHandlerIterator() {
		return this.getHandlers().iterator();
	}

	/**
	 * Subclasses must implement this method to handle Exceptions that occur
	 * while dispatching. The list will never be null, will always have a size
	 * of at least one, and it will be in order with the most recent Exception
	 * at the end. The 'isLast' flag will be <code>true</code> if the Exception
	 * occurred during the final iteration of the MessageHandlers.
	 */
	protected abstract void handleExceptions(
			List<RuntimeException> allExceptions, Message<?> message, boolean isLast);

}
