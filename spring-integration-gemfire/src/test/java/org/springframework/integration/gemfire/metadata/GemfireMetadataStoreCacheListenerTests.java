/*
 * Copyright 2017 the original author or authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.metadata.MetadataStoreListenerAdapter;
import org.springframework.util.Assert;

/**
 * @author Venil Noronha
 *
 * @since 5.0
 *
 */
public class GemfireMetadataStoreCacheListenerTests {

	private static Cache cache;

	private static GemfireMetadataStore metadataStore;

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
			Assert.isTrue(cache.isClosed(), "Cache did not close after close() call");
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
	public void testAdd() throws InterruptedException {
		String testKey = "key";
		String testValue = "value";

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> actualKey = new AtomicReference<>();
		AtomicReference<String> actualValue = new AtomicReference<>();
		metadataStore.addListener(new MetadataStoreListenerAdapter() {

			@Override
			public void onAdd(String key, String value) {
				actualKey.set(key);
				actualValue.set(value);
				latch.countDown();
			}

		});

		metadataStore.put(testKey, testValue);
		latch.await(10, TimeUnit.SECONDS);

		assertEquals(testKey, actualKey.get());
		assertEquals(testValue, actualValue.get());
	}

	@Test
	public void testRemove() throws InterruptedException {
		String testKey = "key";
		String testValue = "value";

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> actualKey = new AtomicReference<>();
		AtomicReference<String> actualValue = new AtomicReference<>();
		metadataStore.addListener(new MetadataStoreListenerAdapter() {

			@Override
			public void onRemove(String key, String oldValue) {
				actualKey.set(key);
				actualValue.set(oldValue);
				latch.countDown();
			}

		});

		metadataStore.put(testKey, testValue);
		metadataStore.remove(testKey);
		latch.await(10, TimeUnit.SECONDS);

		assertEquals(testKey, actualKey.get());
		assertEquals(testValue, actualValue.get());
	}

	@Test
	public void testUpdate() throws InterruptedException {
		String testKey = "key";
		String testValue = "value";
		String testNewValue = "new-value";

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> actualKey = new AtomicReference<>();
		AtomicReference<String> actualValue = new AtomicReference<>();
		metadataStore.addListener(new MetadataStoreListenerAdapter() {

			@Override
			public void onUpdate(String key, String newValue) {
				actualKey.set(key);
				actualValue.set(newValue);
				latch.countDown();
			}

		});

		metadataStore.put(testKey, testValue);
		metadataStore.put(testKey, testNewValue);
		latch.await(10, TimeUnit.SECONDS);

		assertEquals(testKey, actualKey.get());
		assertEquals(testNewValue, actualValue.get());
	}

}
