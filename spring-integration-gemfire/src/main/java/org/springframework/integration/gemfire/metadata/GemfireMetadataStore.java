/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.gemfire.metadata;

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.Scope;

/**
 * Gemfire implementation of {@link ConcurrentMetadataStore}.
 * Use this {@link org.springframework.integration.metadata.MetadataStore}
 * to achieve meta-data persistence shared across application instances and
 * restarts.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class GemfireMetadataStore implements ConcurrentMetadataStore {

	public static final String KEY = "MetaData";

	private final Region<String, String> region;

	public GemfireMetadataStore(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		this.region = cache.<String, String>createRegionFactory().setScope(Scope.LOCAL).create(KEY);
	}

	public GemfireMetadataStore(Region<String, String> region) {
		Assert.notNull(region, "'region' must not be null");
		this.region = region;
	}

	@Override
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		this.region.put(key, value);
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		return this.region.putIfAbsent(key, value);
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(oldValue, "'oldValue' must not be null.");
		Assert.notNull(newValue, "'newValue' must not be null.");
		return this.region.replace(key, oldValue, newValue);
	}

	@Override
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		return this.region.get(key);
	}

	@Override
	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		return this.region.remove(key);
	}

}
