/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.metadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.RedisContainerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 * @since 3.0
 *
 */
class RedisMetadataStoreTests implements RedisContainerTest {

	private static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnection() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@BeforeEach
	@AfterEach
	public void setUpTearDown() {
		RedisContainerTest.createStringRedisTemplate(redisConnectionFactory).delete("testMetadata");
	}

	@Test
	void testGetNonExistingKeyValue() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory);
		String retrievedValue = metadataStore.get("does-not-exist");
		assertThat(retrievedValue).isNull();
	}

	@Test
	void testPersistKeyValue() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-Spring", "Integration");

		StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory);
		BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps("testMetadata");

		assertThat(ops.get("RedisMetadataStoreTests-Spring")).isEqualTo("Integration");
	}

	@Test
	void testGetValueFromMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-GetValue", "Hello Redis");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-GetValue");
		assertThat(retrievedValue).isEqualTo("Hello Redis");
	}

	@Test
	void testPersistEmptyStringToMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");
		metadataStore.put("RedisMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("RedisMetadataStoreTests-PersistEmpty");
		assertThat(retrievedValue).isEmpty();
	}

	@Test
	void testPersistNullStringToMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");

		try {
			metadataStore.put("RedisMetadataStoreTests-PersistEmpty", null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'value' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	@Test
	void testPersistWithEmptyKeyToMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertThat(retrievedValue).isEqualTo("PersistWithEmptyKey");
	}

	@Test
	void testPersistWithNullKeyToMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");

		try {
			metadataStore.put(null, "something");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'key' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void testGetValueWithNullKeyFromMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");

		try {
			metadataStore.get(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'key' must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void testRemoveFromMetadataStore() {
		RedisMetadataStore metadataStore = new RedisMetadataStore(redisConnectionFactory, "testMetadata");

		String testKey = "RedisMetadataStoreTests-Remove";
		String testValue = "Integration";

		metadataStore.put(testKey, testValue);

		assertThat(metadataStore.remove(testKey)).isEqualTo(testValue);
		assertThat(metadataStore.remove(testKey)).isNull();
	}

}
