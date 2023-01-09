/*
 * Copyright 2014-2023 the original author or authors.
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

package org.springframework.integration.gemfire.metadata;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class GemfireMetadataStoreTests {

	private static Cache cache;

	private static ConcurrentMetadataStore metadataStore;

	private static Region<Object, Object> region;

	@BeforeClass
	public static void startUp() throws Exception {
		cache = new CacheFactory().create();
		metadataStore = new GemfireMetadataStore(cache);
		region = cache.getRegion(GemfireMetadataStore.KEY);
	}

	@AfterClass
	public static void cleanUp() {
		if (region != null) {
			region.close();
		}
		if (cache != null) {
			cache.close();
			assertThat(cache.isClosed()).as("Cache did not close after close() call").isTrue();
		}
	}

	@Before
	@After
	public void setup() {
		if (region != null) {
			region.clear();
		}
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = metadataStore.get("does-not-exist");
		assertThat(retrievedValue).isNull();
	}

	@Test
	public void testPersistKeyValue() {
		metadataStore.put("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(region);

		Object v = gemfireTemplate.get("GemfireMetadataStoreTests-Spring");
		assertThat(v).isEqualTo("Integration");
	}

	@Test
	public void testGetValueFromMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-GetValue", "Hello Gemfire");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-GetValue");
		assertThat(retrievedValue).isEqualTo("Hello Gemfire");
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-PersistEmpty");
		assertThat(retrievedValue).isEqualTo("");
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		try {
			metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'value' must not be null.");
		}
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertThat(retrievedValue).isEqualTo("PersistWithEmptyKey");
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		try {
			metadataStore.put(null, "something");
			fail("Expected an IllegalArgumentException to be thrown.");

		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'key' must not be null.");
		}
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
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
	public void testRemoveFromMetadataStore() {
		String testKey = "GemfireMetadataStoreTests-Remove";
		String testValue = "Integration";

		metadataStore.put(testKey, testValue);

		assertThat(metadataStore.remove(testKey)).isEqualTo(testValue);
		assertThat(metadataStore.remove(testKey)).isNull();
	}

	@Test
	public void testPersistKeyValueIfAbsent() {
		metadataStore.putIfAbsent("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(region);

		Object v = gemfireTemplate.get("GemfireMetadataStoreTests-Spring");
		assertThat(v).isEqualTo("Integration");
	}

}
