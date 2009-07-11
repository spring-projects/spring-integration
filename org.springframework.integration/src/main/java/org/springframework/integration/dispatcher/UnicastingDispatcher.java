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

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageDispatcher} that will attempt to send a
 * {@link Message} to at most one of its handlers. The handlers will be tried
 * as determined by the {@link LoadBalancingStrategy} if one is configured. As
 * soon as <em>one</em> of the handlers accepts the Message, the dispatcher will
 * return <code>true</code> and ignore the rest of its handlers.
 * <p/>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers throw Exceptions, the dispatcher will throw an
 * {@link AggregateMessageDeliveryException}.
 * <p/>
 * A load-balancing strategy may be provided to this class to control the order in
 * which the handlers will be tried.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 1.0.2
 */
public class UnicastingDispatcher extends AbstractDispatcher {

	private volatile boolean failover = true;

	private volatile LoadBalancingStrategy loadBalancingStrategy;

	private final TaskExecutor taskExecutor;


	public UnicastingDispatcher() {
		this.taskExecutor = null;
	}

	public UnicastingDispatcher(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}


	/**
	 * Specify whether this dispatcher should failover when a single
	 * {@link MessageHandler} throws an Exception. The default value is
	 * <code>true</code>.
	 */
	public void setFailover(boolean failover) {
		this.failover = failover;
	}

	/**
	 * Provide a {@link LoadBalancingStrategy} for this dispatcher.
	 */
	public void setLoadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
		Assert.notNull(loadBalancingStrategy, "loadBalancingStrategy must not be null");
		this.loadBalancingStrategy = loadBalancingStrategy;
	}

	public final boolean dispatch(final Message<?> message) {
		if (this.taskExecutor != null) {
			this.taskExecutor.execute(new Runnable() {
				public void run() {
					doDispatch(message);
				}
			});
			return true;
		}
		return this.doDispatch(message);
	}

	private boolean doDispatch(Message<?> message) {
		boolean success = false;
		Iterator<MessageHandler> handlerIterator = this.getHandlerIterator(message);
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
	 * Returns the iterator that will be used to loop over the handlers.
	 * Delegates to a {@link LoadBalancingStrategy} if available. Otherwise,
	 * it simply returns the Iterator for the existing handler List.
	 */
	private Iterator<MessageHandler> getHandlerIterator(Message<?> message) {
		if (this.loadBalancingStrategy != null) {
			return this.loadBalancingStrategy.getHandlerIterator(message, this.getHandlers());
		}
		return this.getHandlers().iterator();
	}

	/**
	 * Handles Exceptions that occur while dispatching. If this dispatcher has
	 * failover enabled, it will only throw an Exception when the handler list
	 * is exhausted. The 'isLast' flag will be <emphasis>true</emphasis> if the
	 * Exception occurred during the final iteration of the MessageHandlers.
	 * If failover is disabled for this dispatcher, it will re-throw any
	 * Exception immediately.
	 */
	private void handleExceptions(List<RuntimeException> allExceptions, Message<?> message, boolean isLast) {
		if (isLast || !this.failover) {
			if (allExceptions != null && allExceptions.size() == 1) {
				throw allExceptions.get(0);
			}
			throw new AggregateMessageDeliveryException(message,
					"All attempts to deliver Message to MessageHandlers failed.", allExceptions);
		}
	}

}
