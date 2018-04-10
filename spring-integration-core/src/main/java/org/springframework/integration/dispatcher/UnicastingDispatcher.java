/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.concurrent.Executor;

import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageHandlingRunnable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageDispatcher} that will attempt to send a
 * {@link Message} to at most one of its handlers. The handlers will be tried
 * as determined by the {@link LoadBalancingStrategy} if one is configured. As
 * soon as <em>one</em> of the handlers accepts the Message, the dispatcher will
 * return <code>true</code> and ignore the rest of its handlers.
 * <p>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers throw Exceptions, the dispatcher will throw an
 * {@link AggregateMessageDeliveryException}.
 * <p>
 * A load-balancing strategy may be provided to this class to control the order in
 * which the handlers will be tried.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 1.0.2
 */
public class UnicastingDispatcher extends AbstractDispatcher {

	private final MessageHandler dispatchHandler = message -> doDispatch(message);

	private final Executor executor;

	private volatile boolean failover = true;

	private volatile LoadBalancingStrategy loadBalancingStrategy;

	private volatile MessageHandlingTaskDecorator messageHandlingTaskDecorator = task -> task;

	public UnicastingDispatcher() {
		this.executor = null;
	}

	public UnicastingDispatcher(Executor executor) {
		this.executor = executor;
	}


	/**
	 * Specify whether this dispatcher should failover when a single
	 * {@link MessageHandler} throws an Exception. The default value is
	 * <code>true</code>.
	 *
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		this.failover = failover;
	}

	/**
	 * Provide a {@link LoadBalancingStrategy} for this dispatcher.
	 *
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 */
	public void setLoadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
		this.loadBalancingStrategy = loadBalancingStrategy;
	}

	public void setMessageHandlingTaskDecorator(MessageHandlingTaskDecorator messageHandlingTaskDecorator) {
		Assert.notNull(messageHandlingTaskDecorator, "'messageHandlingTaskDecorator' must not be null.");
		this.messageHandlingTaskDecorator = messageHandlingTaskDecorator;
	}

	@Override
	public final boolean dispatch(final Message<?> message) {
		if (this.executor != null) {
			Runnable task = createMessageHandlingTask(message);
			this.executor.execute(task);
			return true;
		}
		return this.doDispatch(message);
	}

	private Runnable createMessageHandlingTask(final Message<?> message) {
		MessageHandlingRunnable task = new MessageHandlingRunnable() {

			@Override
			public void run() {
				doDispatch(message);
			}

			@Override
			public Message<?> getMessage() {
				return message;
			}

			@Override
			public MessageHandler getMessageHandler() {
				return UnicastingDispatcher.this.dispatchHandler;
			}

		};

		return this.messageHandlingTaskDecorator.decorate(task);
	}

	private boolean doDispatch(Message<?> message) {
		if (tryOptimizedDispatch(message)) {
			return true;
		}
		boolean success = false;
		Iterator<MessageHandler> handlerIterator = this.getHandlerIterator(message);
		if (!handlerIterator.hasNext()) {
			throw new MessageDispatchingException(message, "Dispatcher has no subscribers");
		}
		List<RuntimeException> exceptions = new ArrayList<RuntimeException>();
		while (!success && handlerIterator.hasNext()) {
			MessageHandler handler = handlerIterator.next();
			try {
				handler.handleMessage(message);
				success = true; // we have a winner.
			}
			catch (Exception e) {
				RuntimeException runtimeException = IntegrationUtils.wrapInDeliveryExceptionIfNecessary(message,
						() -> "Dispatcher failed to deliver Message", e);
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
	 * is exhausted. The 'isLast' flag will be <em>true</em> if the
	 * Exception occurred during the final iteration of the MessageHandlers.
	 * If failover is disabled for this dispatcher, it will re-throw any
	 * Exception immediately.
	 */
	private void handleExceptions(List<RuntimeException> allExceptions, Message<?> message, boolean isLast) {
		if (isLast || !this.failover) {
			if (allExceptions != null && allExceptions.size() == 1) {
				throw allExceptions.get(0);
			}
			throw new AggregateMessageDeliveryException(message, //NOSONAR - false positive
					"All attempts to deliver Message to MessageHandlers failed.", allExceptions);
		}
	}

}
