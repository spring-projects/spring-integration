/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin implementation of {@link AbstractWinningHandlerDispatcher}. This
 * implementation will keep track of the index in which its handlers have been
 * tried and return a different handler every dispatch.
 * 
 * @author Iwein Fuld
 */
public class LoadBalancingDispatcher extends AbstractWinningHandlerDispatcher {

	private AtomicInteger currentHandlerIndex = new AtomicInteger();

	/**
	 * Keeps track of the last index over multiple <code>dispatch</code>
	 * invocations. Each invocation of this method will increment the index by
	 * one, overflowing at <code>size</code>. <code>loopIndex</code> is ignored.
	 */
	protected int getNextHandlerIndex(int size, int loopIndex) {
		int indexTail = currentHandlerIndex.getAndIncrement() % size;
		return indexTail < 0 ? indexTail + size : indexTail;
	}
}
