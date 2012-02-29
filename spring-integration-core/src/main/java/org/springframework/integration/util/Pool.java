/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Represents a pool of items.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface Pool<T> {

	/**
	 * Obtains an item from the pool.
	 * @return the item.
	 */
	T getItem();

	/**
	 * Returns an item to the pool.
	 * @param t the item.
	 */
	void returnItem(T t);

	/**
	 * Returns the current size (limit) of the pool.
	 * @return the size.
	 */
	int getPoolSize();

	/**
	 * Returns the number of items that have been allocated
	 * but are not in use.
	 * @return The number of items.
	 */
	int getIdleSize();

	/**
	 * Returns the number of allocated items that are currently
	 * checked out of the pool.
	 * @return The number of items.
	 */
	int getInUseSize();

	/**
	 * Removes all idle items from the pool.
	 */
	void releaseAll();

}
