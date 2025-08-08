/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
