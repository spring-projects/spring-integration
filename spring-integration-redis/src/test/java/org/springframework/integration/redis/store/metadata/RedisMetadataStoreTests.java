/*
 * Copyright 2013 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.store.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

/**
 * @author Gunnar Hillert
 * @since 3.0
 *
 */
public class RedisMetadataStoreTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testGetNonExistingKeyValue(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		String retrievedValue = metadataStore.get("does-not-exist");
		assertNull(retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistKeyValue(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		metadataStore.put("RedisMetadataStoreTests-Spring", "Integration");

		StringRedisTemplate redisTemplate = new StringRedisTemplate(jcf);
		BoundValueOperations<String, String> ops = redisTemplate.boundValueOps("RedisMetadataStoreTests-Spring");

		assertEquals("Integration", ops.get());
	}

	@Test
	@RedisAvailable
	public void testGetValueFromMetadataStore(){

		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		metadataStore.put("RedisMetadataStoreTests-GetValue", "Hello Redis");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-GetValue");
		assertEquals("Hello Redis", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistEmptyStringToMetadataStore(){

		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		metadataStore.put("RedisMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-PersistEmpty");
		assertEquals("", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistNullStringToMetadataStore(){

		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);

		try {
			metadataStore.put("RedisMetadataStoreTests-PersistEmpty", null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'value' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	@Test
	@RedisAvailable
	public void testPersistWithEmptyKeyToMetadataStore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertEquals("PersistWithEmptyKey", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistWithNullKeyToMetadataStore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);

		try {
			metadataStore.put(null, "something");
		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@RedisAvailable
	public void testGetValueWithNullKeyFromMetadataStore(){
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);

		try {
			metadataStore.get(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}
}
