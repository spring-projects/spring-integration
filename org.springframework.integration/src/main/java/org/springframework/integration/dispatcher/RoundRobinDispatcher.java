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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * Round-robin implementation of {@link AbstractUnicastDispatcher}. This
 * implementation will keep track of the index of the handler that has been
 * tried first and use a different starting handler every dispatch.
 * 
 * @author Iwein Fuld
 */
public class RoundRobinDispatcher extends AbstractUnicastDispatcher {

	private volatile boolean failover = true;

	private final AtomicInteger currentHandlerIndex = new AtomicInteger();


	public void setFailover(boolean failover) {
		this.failover = failover;
	}

	/**
	 * Returns an iterator that starts at a new point in the list every time the
	 * first part of the list that is skipped will be used at the end of the
	 * iteration, so it guarantees all handlers are returned once on subsequent
	 * <code>next()</code> invocations.
	 */
	@Override
	protected Iterator<MessageHandler> getHandlerIterator() {
		List<MessageHandler> handlers = this.getHandlers();
		int size = handlers.size();
		if (size == 0) {
			return handlers.iterator();
		}
		int nextHandlerStartIndex = getNextHandlerStartIndex(size);
		List<MessageHandler> reorderedHandlers = new ArrayList<MessageHandler>(
				handlers.subList(nextHandlerStartIndex, size));
		reorderedHandlers.addAll(handlers.subList(0, nextHandlerStartIndex));
		return reorderedHandlers.iterator();
	}

	/**
	 * Keeps track of the last index over multiple dispatches. Each invocation
	 * of this method will increment the index by one, overflowing at
	 * <code>size</code>.
	 */
	private int getNextHandlerStartIndex(int size) {
		int indexTail = currentHandlerIndex.getAndIncrement() % size;
		return indexTail < 0 ? indexTail + size : indexTail;
	}

	@Override
	protected void handleExceptions(List<RuntimeException> allExceptions,
			Message<?> message, boolean isLast) {
		if (isLast || !this.failover) {
			if (allExceptions != null && allExceptions.size() == 1) {
				throw allExceptions.get(0);
			}
			throw new AggregateMessageDeliverException(message,
					"Failed to deliver Message to any MessageHandler.", allExceptions);
		}
	}

}
