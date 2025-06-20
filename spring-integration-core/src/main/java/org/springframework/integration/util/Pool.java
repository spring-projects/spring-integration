/*
 * Copyright 2002-present the original author or authors.
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

/**
 * Represents a pool of items.
 *
 * @param <T> pool element type.
 *
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public interface Pool<T> {

	/**
	 * Obtain an item from the pool.
	 * @return the item.
	 */
	T getItem();

	/**
	 * Release an item back into the pool. This must be an item that
	 * was previously retrieved using {@link #getItem()}.
	 * @param t the item.
	 * @throws IllegalArgumentException when a "foreign" object
	 * is released.
	 */
	void releaseItem(T t);

	/**
	 * Remove all idle items from the pool.
	 */
	void removeAllIdleItems();

	/**
	 * Return the current size (limit) of the pool.
	 * @return the size.
	 */
	int getPoolSize();

	/**
	 * Return the number of items that have been allocated
	 * but are not currently in use.
	 * @return The number of items.
	 */
	int getIdleCount();

	/**
	 * Return the number of allocated items that are currently
	 * checked out of the pool.
	 * @return The number of items.
	 */
	int getActiveCount();

	/**
	 * Return the current count of allocated items (in use and
	 * idle). May be less than the pool size, and reflects the
	 * high watermark of pool usage.
	 * @return the number of items.
	 */
	int getAllocatedCount();

	/**
	 * Close the pool; returned items will be destroyed.
	 * @since 4.3.23
	 */
	default void close() {
	}

}
