/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link MessageDispatcher} implementations.
 * <p>
 * The subclasses implement the actual dispatching strategy, but this base class
 * manages the registration of {@link MessageHandler}s. Although the implemented
 * dispatching strategies may invoke handles in different ways (e.g. round-robin
 * vs. failover), this class does maintain the order of the underlying
 * collection. See the {@link OrderedAwareLinkedHashSet} for more detail.
 * 
 * Modification of the internal handler list is guarded by a
 * {@link ReadWriteLock}.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public abstract class AbstractDispatcher implements MessageDispatcher {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Set<MessageHandler> handlers = new OrderedAwareLinkedHashSet<MessageHandler>();

	private final ReadWriteLock handlersLock = new ReentrantReadWriteLock();

	/**
	 * Returns a copied, unmodifiable List of this dispatcher's handlers. This
	 * is provided for access by subclasses.
	 */
	protected List<MessageHandler> getHandlers() {
		handlersLock.readLock().lock();
		ArrayList<MessageHandler> newList = new ArrayList<MessageHandler>(this.handlers);
		handlersLock.readLock().unlock();
		return Collections.unmodifiableList(newList);
	}

	/**
	 * Add the handler to the internal Set.
	 * 
	 * @return the result of {@link Set#add(Object)}
	 */
	public boolean addHandler(MessageHandler handler) {
		handlersLock.writeLock().lock();
		boolean added = this.handlers.add(handler);
		handlersLock.writeLock().unlock();
		return added;
	}

	/**
	 * Remove the handler from the internal handler Set.
	 * 
	 * @return the result of {@link Set#remove(Object)}
	 */
	public boolean removeHandler(MessageHandler handler) {
		handlersLock.writeLock().lock();
		boolean removed = this.handlers.remove(handler);
		handlersLock.writeLock().unlock();
		return removed;
	}

	public String toString() {
		handlersLock.readLock().lock();
		String handlerList = StringUtils
				.collectionToCommaDelimitedString(this.handlers);
		handlersLock.readLock().unlock();
		return this.getClass().getSimpleName() + " with handlers: "
				+ handlerList;
	}

}
