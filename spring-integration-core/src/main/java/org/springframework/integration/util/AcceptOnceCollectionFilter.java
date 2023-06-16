/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of {@link CollectionFilter} that remembers the elements passed in
 * the previous invocation in order to avoid returning those elements more than once.
 *
 * @param <T> the collection element type.
 *
 * @author Mark Fisher
 * @author Christian Tzolov
 *
 * @since 2.1
 */
public class AcceptOnceCollectionFilter<T> implements CollectionFilter<T> {

	private volatile Collection<T> lastSeenElements = Collections.emptyList();

	private final Lock lock = new ReentrantLock();

	public Collection<T> filter(Collection<T> unfilteredElements) {
		this.lock.lock();
		try {
			List<T> filteredElements = new ArrayList<>();
			for (T element : unfilteredElements) {
				if (!this.lastSeenElements.contains(element)) {
					filteredElements.add(element);
				}
			}
			this.lastSeenElements = unfilteredElements;
			return filteredElements;
		}
		finally {
			this.lock.unlock();
		}
	}

}
