/*
 * Copyright 2013-2014 the original author or authors
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
package org.springframework.integration.redis.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 *
 */
public class RedisMetadataStoreTests extends RedisAvailableTests {

	@Before
	@After
	public void setUpTearDown() {
		this.createStringRedisTemplate(this.getConnectionFactoryForTest()).delete("testMetadata");
	}

	@Test
	@RedisAvailable
	public void testGetNonExistingKeyValue() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf);
		String retrievedValue = metadataStore.get("does-not-exist");
		assertNull(retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistKeyValue() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-Spring", "Integration");

		StringRedisTemplate redisTemplate = new StringRedisTemplate(jcf);
		BoundHashOperations<String,Object,Object> ops = redisTemplate.boundHashOps("testMetadata");

		assertEquals("Integration", ops.get("RedisMetadataStoreTests-Spring"));
	}

	@Test
	@RedisAvailable
	public void testGetValueFromMetadataStore() {

		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-GetValue", "Hello Redis");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-GetValue");
		assertEquals("Hello Redis", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistEmptyStringToMetadataStore() {

		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-PersistEmpty");
		assertEquals("", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistNullStringToMetadataStore() {

		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");

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
	public void testPersistWithEmptyKeyToMetadataStore() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertEquals("PersistWithEmptyKey", retrievedValue);
	}

	@Test
	@RedisAvailable
	public void testPersistWithNullKeyToMetadataStore() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");

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
	public void testGetValueWithNullKeyFromMetadataStore() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");

		try {
			metadataStore.get(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@RedisAvailable
	public void testRemoveFromMetadataStore() {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMetadataStore metadataStore = new RedisMetadataStore(jcf, "testMetadata");

		String testKey = "RedisMetadataStoreTests-Remove";
		String testValue = "Integration";

		metadataStore.put(testKey, testValue);

		assertEquals(testValue, metadataStore.remove(testKey));
		assertNull(metadataStore.remove(testKey));
	}

}
