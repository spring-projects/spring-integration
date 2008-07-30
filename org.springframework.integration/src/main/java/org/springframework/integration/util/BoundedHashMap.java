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

package org.springframework.integration.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * A Map implementation that enforces the specified capacity.
 *  
 * @author Mark Fisher
 */
public class BoundedHashMap<K, V> extends LinkedHashMap<K, V> {

	private static final Log logger = LogFactory.getLog(BoundedHashMap.class);

	private final int capacity;


	public BoundedHashMap(int capacity) {
		Assert.isTrue(capacity > 0, "capacity must be a positive value");
		this.capacity = capacity;
	}


	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		boolean shouldRemove = this.size() > this.capacity;
		if (shouldRemove && logger.isDebugEnabled()) {
			logger.debug("removing eldest entry from BoundedHashMap with capacity of " + this.capacity);
		}
		return shouldRemove;
	}

}
