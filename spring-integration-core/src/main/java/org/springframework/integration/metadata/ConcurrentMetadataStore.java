/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.metadata;

/**
 * Supports atomic updates to values in the store.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface ConcurrentMetadataStore extends MetadataStore {

	/**
	 * Atomically insert the key into the store.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return null if successful, the old value otherwise.
	 */
	String putIfAbsent(String key, String value);

	/**
	 * Atomically replace the value for the key in the store if the old
	 * value matches the oldValue argument.
	 *
	 * @param key The key.
	 * @param oldValue The old value.
	 * @param newValue The new value.
	 * @return true if successful.
	 */
	boolean replace(String key, String oldValue, String newValue);

}
