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

package org.springframework.integration.metadata;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Strategy interface for storing metadata from certain adapters
 * to avoid duplicate delivery of messages, for example.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public interface MetadataStore {

	/**
	 * Writes a key value pair to this MetadataStore.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	void put(String key, String value);

	/**
	 * Reads a value for the given key from this MetadataStore.
	 *
	 * @param key The key.
	 * @return The value.
	 */
	@ManagedAttribute
	String get(String key);

	/**
	 * Remove a value for the given key from this MetadataStore.
	 * @param key The key.
	 * @return The previous value associated with key, or
	 *         null if there was no mapping for key.
	 */
	@ManagedAttribute
	String remove(String key);

}
