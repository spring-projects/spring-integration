/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link CollectionFilter} that remembers the elements passed in
 * the previous invocation in order to avoid returning those elements more than once.
 * 
 * @author Mark Fisher
 * @since 2.1
 */
public class AcceptOnceCollectionFilter<T> implements CollectionFilter<T> {

	private volatile Collection<T> lastSeenElements = Collections.emptyList();

	public synchronized Collection<T> filter(Collection<T> unfilteredElements) {
		List<T> filteredElements = new ArrayList<T>();
		for (T element : unfilteredElements) {
			if (!this.lastSeenElements.contains(element)) {
				filteredElements.add(element);
			}
		}
		this.lastSeenElements = unfilteredElements;
		return filteredElements;
	}

}
