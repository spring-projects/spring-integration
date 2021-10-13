/*
 * Copyright 2015-2021 the original author or authors.
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
 * A callback to be invoked whenever a value changes in the data store.
 *
 * @author Marius Bogoevici
 *
 * @since 4.2
 */
public interface MetadataStoreListener {

	/**
	 * Invoked when a key is added to the store.
	 * @param key the key being added
	 * @param value the value being added
	 */
	void onAdd(String key, String value);

	/**
	 * Invoked when a key is removed from the store.
	 * @param key the key being removed
	 * @param oldValue the value being removed
	 */
	void onRemove(String key, String oldValue);

	/**
	 * Invoked when a key is updated into the store.
	 * @param key the key being updated
	 * @param newValue the new value
	 */
	void onUpdate(String key, String newValue);

}
