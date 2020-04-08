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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Round-robin implementation of {@link LoadBalancingStrategy}. This
 * implementation will keep track of the index of the handler that has been
 * tried first and use a different starting handler every dispatch.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class RoundRobinLoadBalancingStrategy implements LoadBalancingStrategy {

	private final AtomicInteger currentHandlerIndex = new AtomicInteger();

	/**
	 * Returns an iterator that starts at a new point in the collection every time the
	 * first part of the list that is skipped will be used at the end of the
	 * iteration, so it guarantees all handlers are returned once on subsequent
	 * <code>next()</code> invocations.
	 */
	public final Iterator<MessageHandler> getHandlerIterator(Message<?> message, Collection<MessageHandler> handlers) {
		int size = handlers.size();
		if (size < 2) {
			this.getNextHandlerStartIndex(size);
			return handlers.iterator();
		}

		return buildHandlerIterator(size, handlers.toArray(new MessageHandler[size]));
	}

	private Iterator<MessageHandler> buildHandlerIterator(int size, final MessageHandler[] handlers) {
		int nextHandlerStartIndex = getNextHandlerStartIndex(size);

		MessageHandler[] reorderedHandlers = new MessageHandler[size];

		System.arraycopy(handlers, nextHandlerStartIndex, reorderedHandlers, 0, size - nextHandlerStartIndex);
		System.arraycopy(handlers, 0, reorderedHandlers, size - nextHandlerStartIndex, nextHandlerStartIndex);

		return Arrays.stream(reorderedHandlers).iterator();
	}

	/**
	 * Keeps track of the last index over multiple dispatches. Each invocation
	 * of this method will increment the index by one, overflowing at
	 * <code>size</code>.
	 */
	private int getNextHandlerStartIndex(int size) {
		if (size > 0) {
			int indexTail = this.currentHandlerIndex.getAndIncrement() % size;
			return indexTail < 0 ? indexTail + size : indexTail;
		}
		else {
			return size;
		}
	}

}
