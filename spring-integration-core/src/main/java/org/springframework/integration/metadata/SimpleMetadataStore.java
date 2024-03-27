/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.util.Assert;

/**
 * Simple implementation of {@link MetadataStore} that uses a {@link ConcurrentMap} for the data store.
 * The metadata may not be persisted across application restarts, if the provided {@link ConcurrentMap}
 * is an in-memory instance.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class SimpleMetadataStore implements ConcurrentMetadataStore {

	private final ConcurrentMap<String, String> metadata;

	/**
	 * Instantiate a {@link SimpleMetadataStore} using an in-memory {@link ConcurrentHashMap}.
	 */
	public SimpleMetadataStore() {
		this(new ConcurrentHashMap<String, String>());
	}

	/**
	 * Instantiate a {@link SimpleMetadataStore} using the provided {@link ConcurrentMap}.
	 * The implementation may be a distributed map provided by projects such as Redis and Hazelcast.
	 * @param metadata the {@link ConcurrentMap} instance for metadata.
	 * @since 4.1.4
	 */
	public SimpleMetadataStore(ConcurrentMap<String, String> metadata) {
		Assert.notNull(metadata, "'metadata' must not be null.");
		this.metadata = metadata;
	}

	@Override
	public void put(String key, String value) {
		this.metadata.put(key, value);
	}

	@Override
	public String get(String key) {
		return this.metadata.get(key);
	}

	@Override
	public String remove(String key) {
		return this.metadata.remove(key);
	}

	@Override
	public String putIfAbsent(String key, String value) {
		return this.metadata.putIfAbsent(key, value);
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		return this.metadata.replace(key, oldValue, newValue);
	}

}
