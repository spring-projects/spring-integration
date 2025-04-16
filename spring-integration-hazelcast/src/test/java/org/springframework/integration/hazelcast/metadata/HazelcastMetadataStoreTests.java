/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.integration.hazelcast.metadata;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.MetadataStoreListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Vinicius Carvalho
 * @author Artem Bilan
 */
public class HazelcastMetadataStoreTests {

	private static HazelcastInstance instance;

	private static IMap<String, String> map;

	HazelcastMetadataStore metadataStore;

	@BeforeAll
	public static void init() {
		instance = Hazelcast.newHazelcastInstance();
		map = instance.getMap("customTestsMetadataStore");
	}

	@AfterAll
	public static void destroy() {
		instance.shutdown();
	}

	@BeforeEach
	public void setup() {
		this.metadataStore = new HazelcastMetadataStore(map);
		this.metadataStore.afterPropertiesSet();
	}

	@AfterEach
	public void clean() {
		map.clear();
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = this.metadataStore.get("does-not-exist");
		assertThat(retrievedValue).isNull();
	}

	@Test
	public void testPersistKeyValue() {
		this.metadataStore.put("HazelcastMetadataStoreTests-Spring", "Integration");
		assertThat(map.get("HazelcastMetadataStoreTests-Spring")).isEqualTo("Integration");
	}

	@Test
	public void testGetValueFromMetadataStore() {
		this.metadataStore.put("HazelcastMetadataStoreTests-GetValue", "Hello Hazelcast");
		String retrievedValue = this.metadataStore.get("HazelcastMetadataStoreTests-GetValue");
		assertThat(retrievedValue).isEqualTo("Hello Hazelcast");
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		this.metadataStore.put("HazelcastMetadataStoreTests-PersistEmpty", "");
		String retrievedValue = this.metadataStore.get("HazelcastMetadataStoreTests-PersistEmpty");
		assertThat(retrievedValue).isEqualTo("");
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.metadataStore.put("HazelcastMetadataStoreTests-PersistEmpty", null))
				.withMessage("'value' must not be null.");
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		this.metadataStore.put("", "PersistWithEmptyKey");
		String retrievedValue = this.metadataStore.get("");
		assertThat(retrievedValue).isEqualTo("PersistWithEmptyKey");
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.metadataStore.put(null, "something"))
				.withMessage("'key' must not be null.");
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.metadataStore.get(null))
				.withMessage("'key' must not be null.");
	}

	@Test
	public void testRemoveFromMetadataStore() {
		String testKey = "HazelcastMetadataStoreTests-Remove";
		String testValue = "Integration";

		this.metadataStore.put(testKey, testValue);

		assertThat(this.metadataStore.remove(testKey)).isEqualTo(testValue);
		assertThat(this.metadataStore.remove(testKey)).isNull();
	}

	@Test
	public void testPersistKeyValueIfAbsent() {
		this.metadataStore.putIfAbsent("HazelcastMetadataStoreTests-Spring", "Integration");
		assertThat(map.get("HazelcastMetadataStoreTests-Spring")).isEqualTo("Integration");
	}

	@Test
	public void testReplaceValue() {
		this.metadataStore.put("key", "old");
		assertThat(map.get("key")).isEqualTo("old");
		this.metadataStore.replace("key", "old", "new");
		assertThat(map.get("key")).isEqualTo("new");
	}

	@Test
	public void testListener() {
		MetadataStoreListener listener = mock(MetadataStoreListener.class);
		this.metadataStore.addListener(listener);

		this.metadataStore.put("foo", "bar");
		this.metadataStore.replace("foo", "bar", "baz");
		this.metadataStore.remove("foo");

		await().untilAsserted(() -> verify(listener).onAdd("foo", "bar"));
		verify(listener).onUpdate("foo", "baz");
		verify(listener).onRemove("foo", "baz");
	}

}
