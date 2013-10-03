/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.redis.store.metadata;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.store.metadata.MetadataStore;
import org.springframework.util.Assert;

/**
 * Redis implementation of {@link MetadataStore}. Use this {@link MetadataStore}
 * to achieve meta-data persistence across application restarts.
 *
 * @author Gunnar Hillert
 * @since 3.0
 */
public class RedisMetadataStore implements MetadataStore {

	private final StringRedisTemplate redisTemplate;

	/**
	 * Initializes the {@link RedisTemplate}.
	 * A {@link StringRedisTemplate} is used with default properties.
	 *
	 * @param connectionFactory Must not be null
	 */
	public RedisMetadataStore(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null.");
		this.redisTemplate = new StringRedisTemplate(connectionFactory);
	}

	/**
	 * Persists the provided key and value to Redis.
	 *
	 * @param key Must not be null
	 * @param value Must not be null
	 */
	public void put(String key, String value) {
		Assert.notNull(key, "'key' must not be null.");
		Assert.notNull(value, "'value' must not be null.");
		BoundValueOperations<String, String> ops = this.redisTemplate.boundValueOps(key);
		ops.set(value);
	}

	/**
	 * Retrieve the persisted value for the provided key.
	 *
	 * @param key Must not be null
	 */
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		BoundValueOperations<String, String> ops = this.redisTemplate.boundValueOps(key);
		return ops.get();
	}
}
