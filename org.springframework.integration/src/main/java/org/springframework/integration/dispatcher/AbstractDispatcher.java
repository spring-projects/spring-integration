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
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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

	private final ReentrantReadWriteLock handlersLock = new ReentrantReadWriteLock();

	private final ReadLock readLock = handlersLock.readLock();

	private final WriteLock writeLock = handlersLock.writeLock();


	/**
	 * Returns a copied, unmodifiable List of this dispatcher's handlers. This
	 * is provided for access by subclasses.
	 */
	protected List<MessageHandler> getHandlers() {
		ArrayList<MessageHandler> newList = null;
		readLock.lock();
		try {
			newList = new ArrayList<MessageHandler>(this.handlers);
		}
		finally {
			readLock.unlock();
		}
		return Collections.unmodifiableList(newList);
	}

	/**
	 * Add the handler to the internal Set.
	 * 
	 * @return the result of {@link Set#add(Object)}
	 */
	public boolean addHandler(MessageHandler handler) {
		writeLock.lock();
		try {
			return this.handlers.add(handler);
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * Remove the handler from the internal handler Set.
	 * 
	 * @return the result of {@link Set#remove(Object)}
	 */
	public boolean removeHandler(MessageHandler handler) {
		writeLock.lock();
		try {
			return this.handlers.remove(handler);
		}
		finally {
			writeLock.unlock();
		}
	}

	public String toString() {
		String handlerList = null;
		readLock.lock();
		try {
			handlerList = StringUtils.collectionToCommaDelimitedString(this.handlers);
		}
		finally {
			readLock.unlock();
		}
		return this.getClass().getSimpleName() + " with handlers: " + handlerList;
	}

}
