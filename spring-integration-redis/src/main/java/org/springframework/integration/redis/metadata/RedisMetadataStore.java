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

package org.springframework.integration.redis.metadata;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisProperties;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.util.Assert;

/**
 * Redis implementation of {@link MetadataStore}. Use this {@link MetadataStore}
 * to achieve meta-data persistence across application restarts.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
public class RedisMetadataStore implements MetadataStore {

	public static final String KEY = "MetaData";

	private final RedisProperties properties;

	/**
	 * Specifies the {@link RedisProperties} backend for this {@link MetadataStore}.
	 */
	public RedisMetadataStore(RedisProperties properties) {
		Assert.notNull(properties, "'properties' must not be null.");
		this.properties = properties;
	}

	/**
	 * Initializes the {@link RedisProperties} by provided {@link RedisConnectionFactory}
	 * and default hash key - {@link #KEY}.
	 */
	public RedisMetadataStore(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, KEY);
	}

	/**
	 * Initializes the {@link RedisProperties} by provided {@link RedisConnectionFactory} and key.
	 */
	public RedisMetadataStore(RedisConnectionFactory connectionFactory, String key) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null.");
		Assert.hasText(key, "'key' must not be empty.");
		RedisOperations<String, String> redisTemplate = new StringRedisTemplate(connectionFactory);
		BoundHashOperations<String, String, String> hashOperations = redisTemplate.boundHashOps(key);
		this.properties = new RedisProperties(hashOperations);
	}

	/**
	 * Initializes the {@link RedisProperties} by provided {@link RedisConnectionFactory}
	 * and default hash key - {@link #KEY}.
	 */
	public RedisMetadataStore(RedisOperations<String, ?> operations) {
		this(operations, KEY);
	}

	/**
	 * Initializes the {@link RedisProperties} by provided {@link RedisConnectionFactory} and key.
	 */
	public RedisMetadataStore(RedisOperations<String, ?> operations, String key) {
		Assert.notNull(operations, "'operations' must not be null.");
		Assert.hasText(key, "'key' must not be empty.");
		BoundHashOperations<String, String, String> hashOperations = operations.boundHashOps(key);
		this.properties = new RedisProperties(hashOperations);
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
		this.properties.put(key, value);
	}

	/**
	 * Retrieve the persisted value for the provided key.
	 *
	 * @param key Must not be null
	 */
	public String get(String key) {
		Assert.notNull(key, "'key' must not be null.");
		return (String) this.properties.get(key);
	}

	@Override

	public String remove(String key) {
		Assert.notNull(key, "'key' must not be null.");
		return (String) this.properties.remove(key);
	}

}
