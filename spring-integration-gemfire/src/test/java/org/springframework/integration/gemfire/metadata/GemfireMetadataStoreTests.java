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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.integration.gemfire.metadata.GemfireMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;

/**
 * @author Artem Bilan
 * @since 4.0
 *
 */
public class GemfireMetadataStoreTests {

	public static Cache cache;

	public static ConcurrentMetadataStore metadataStore;

	@BeforeClass
	public static void startUp() throws Exception {
		cache = new CacheFactory().create();
		metadataStore = new GemfireMetadataStore(cache);
	}

	@AfterClass
	public static void cleanUp() {
		cache.close();
		Assert.isTrue(cache.isClosed(), "Cache did not close after close() call");
	}

	@Before
	@After
	public void setup() {
		getRegion().clear();
	}

	@Test
	public void testGetNonExistingKeyValue() {
		String retrievedValue = metadataStore.get("does-not-exist");
		assertNull(retrievedValue);
	}

	@Test
	public void testPersistKeyValue() {
		metadataStore.put("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(getRegion());

		assertEquals("Integration", gemfireTemplate.get("GemfireMetadataStoreTests-Spring"));
	}

	@Test
	public void testGetValueFromMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-GetValue", "Hello Gemfire");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-GetValue");
		assertEquals("Hello Gemfire", retrievedValue);
	}

	@Test
	public void testPersistEmptyStringToMetadataStore() {
		metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", "");

		String retrievedValue = metadataStore.get("GemfireMetadataStoreTests-PersistEmpty");
		assertEquals("", retrievedValue);
	}

	@Test
	public void testPersistNullStringToMetadataStore() {
		try {
			metadataStore.put("GemfireMetadataStoreTests-PersistEmpty", null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("'value' must not be null.", e.getMessage());
		}
	}

	@Test
	public void testPersistWithEmptyKeyToMetadataStore() {
		metadataStore.put("", "PersistWithEmptyKey");

		String retrievedValue = metadataStore.get("");
		assertEquals("PersistWithEmptyKey", retrievedValue);
	}

	@Test
	public void testPersistWithNullKeyToMetadataStore() {
		try {
			metadataStore.put(null, "something");
			fail("Expected an IllegalArgumentException to be thrown.");

		}
		catch (IllegalArgumentException e) {
			assertEquals("'key' must not be null.", e.getMessage());
		}
	}

	@Test
	public void testGetValueWithNullKeyFromMetadataStore() {
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
	public void testRemoveFromMetadataStore() {
		String testKey = "GemfireMetadataStoreTests-Remove";
		String testValue = "Integration";

		metadataStore.put(testKey, testValue);

		assertEquals(testValue, metadataStore.remove(testKey));
		assertNull(metadataStore.remove(testKey));
	}

	@Test
	public void testPersistKeyValueIfAbsent() {
		metadataStore.putIfAbsent("GemfireMetadataStoreTests-Spring", "Integration");

		GemfireTemplate gemfireTemplate = new GemfireTemplate(getRegion());

		assertEquals("Integration", gemfireTemplate.get("GemfireMetadataStoreTests-Spring"));
	}

	private static Region<Object, Object> getRegion() {
		return cache.getRegion(GemfireMetadataStore.KEY);
	}

}
