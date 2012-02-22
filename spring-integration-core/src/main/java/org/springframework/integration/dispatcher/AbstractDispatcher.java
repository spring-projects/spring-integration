/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.MessageHandler;

/**
 * Base class for {@link MessageDispatcher} implementations.
 * <p>
 * The subclasses implement the actual dispatching strategy, but this base class
 * manages the registration of {@link MessageHandler}s. Although the implemented
 * dispatching strategies may invoke handles in different ways (e.g. round-robin
 * vs. failover), this class does maintain the order of the underlying
 * collection. See the {@link OrderedAwareLinkedHashSet} for more detail.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractDispatcher implements MessageDispatcher {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Set<MessageHandler> handlers = new OrderedAwareLinkedHashSet<MessageHandler>();


	/**
	 * Returns a copied, unmodifiable List of this dispatcher's handlers. This
	 * is provided for access by subclasses.
	 */
	protected List<MessageHandler> getHandlers() {
		return Collections.<MessageHandler>unmodifiableList(Arrays.<MessageHandler>asList(
				this.handlers.toArray(new MessageHandler[this.handlers.size()])));
	}

	/**
	 * Add the handler to the internal Set.
	 * 
	 * @return the result of {@link Set#add(Object)}
	 */
	public boolean addHandler(MessageHandler handler) {
		return this.handlers.add(handler);
	}

	/**
	 * Remove the handler from the internal handler Set.
	 * 
	 * @return the result of {@link Set#remove(Object)}
	 */
	public boolean removeHandler(MessageHandler handler) {
		return this.handlers.remove(handler);
	}

	public String toString() {
		return this.getClass().getSimpleName() + " with handlers: " + this.handlers.toString();
	}

	/**
	 * @return The current number of handlers
	 */
	public int getHandlerCount() {
		return this.handlers.size();
	}
}
