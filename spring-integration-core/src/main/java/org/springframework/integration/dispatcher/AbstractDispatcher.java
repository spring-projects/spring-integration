/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessageDispatcher} implementations.
 * <p>
 * The subclasses implement the actual dispatching strategy, but this base class
 * manages the registration of {@link MessageHandler}s. Although the implemented
 * dispatching strategies may invoke handles in different ways (e.g. round-robin
 * vs. failover), this class does maintain the order of the underlying
 * collection. See the {@link OrderedAwareCopyOnWriteArraySet} for more detail.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Diego Belfer
 * @author Artem Bilan
 */
public abstract class AbstractDispatcher implements MessageDispatcher {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final OrderedAwareCopyOnWriteArraySet<MessageHandler> handlers = new OrderedAwareCopyOnWriteArraySet<>();

	private volatile int maxSubscribers = Integer.MAX_VALUE;

	private volatile MessageHandler theOneHandler;

	/**
	 * Set the maximum subscribers allowed by this dispatcher.
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
	}

	/**
	 * Returns an unmodifiable {@link Set} of this dispatcher's handlers. This
	 * is provided for access by subclasses.
	 *
	 * @return The message handlers.
	 */
	protected Set<MessageHandler> getHandlers() {
		return this.handlers.asUnmodifiableSet();
	}

	/**
	 * Add the handler to the internal Set.
	 *
	 * @param handler The handler to add.
	 * @return the result of {@link Set#add(Object)}
	 */
	@Override
	public synchronized boolean addHandler(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		Assert.isTrue(this.handlers.size() < this.maxSubscribers, "Maximum subscribers exceeded");
		boolean added = this.handlers.add(handler);
		if (this.handlers.size() == 1) {
			this.theOneHandler = handler;
		}
		else {
			this.theOneHandler = null;
		}
		return added;
	}

	/**
	 * Remove the handler from the internal handler Set.
	 *
	 * @return the result of {@link Set#remove(Object)}
	 */
	@Override
	public synchronized boolean removeHandler(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		boolean removed = this.handlers.remove(handler);
		if (this.handlers.size() == 1) {
			this.theOneHandler = this.handlers.iterator().next();
		}
		else {
			this.theOneHandler = null;
		}
		return removed;
	}

	protected boolean tryOptimizedDispatch(Message<?> message) {
		MessageHandler handler = this.theOneHandler;
		if (handler != null) {
			try {
				handler.handleMessage(message);
				return true;
			}
			catch (Exception e) {
				throw IntegrationUtils.wrapInDeliveryExceptionIfNecessary(message,
						() -> "Dispatcher failed to deliver Message", e);
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " with handlers: " + this.handlers.toString();
	}

	@Override
	public int getHandlerCount() {
		return this.handlers.size();
	}

}
