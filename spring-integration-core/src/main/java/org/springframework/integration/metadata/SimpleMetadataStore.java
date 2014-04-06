/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.metadata;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Simple implementation of {@link MetadataStore} that uses an in-memory map only.
 * The metadata will not be persisted across application restarts.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SimpleMetadataStore implements ConcurrentMetadataStore {

	private final ConcurrentMap<String, String> metadata = new ConcurrentHashMap<String, String>();


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
