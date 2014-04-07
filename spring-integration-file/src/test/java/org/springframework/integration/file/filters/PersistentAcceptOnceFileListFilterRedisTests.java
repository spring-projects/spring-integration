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
package org.springframework.integration.file.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class PersistentAcceptOnceFileListFilterRedisTests extends RedisAvailableTests {

	@Before
	@After
	public void setupShutDown() {
		RedisTemplate<String, ?> template = this.createTemplate();
		template.delete("persistentAcceptOnceFileListFilterRedisTests");
	}

	private RedisTemplate<String, ?> createTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(this.getConnectionFactoryForTest());
		template.setKeySerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}

	@Test
	@RedisAvailable
	public void testFileSystem() throws Exception {
		final AtomicBoolean suspend = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		RedisMetadataStore store = new RedisMetadataStore(this.getConnectionFactoryForTest(),
				"persistentAcceptOnceFileListFilterRedisTests") {

			@Override
			public boolean replace(String key, String oldValue, String newValue) {
				if (suspend.get()) {
					latch2.countDown();
					try {
						latch1.await(10, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return super.replace(key, oldValue, newValue);
			}

		};
		final FileSystemPersistentAcceptOnceFileListFilter filter =
				new FileSystemPersistentAcceptOnceFileListFilter(store,"foo:");
		final File file = File.createTempFile("foo", ".txt");
		assertEquals(1, filter.filterFiles(new File[] {file}).size());
		String ts = store.get("foo:" + file.getAbsolutePath());
		assertEquals(String.valueOf(file.lastModified()), ts);
		assertEquals(0, filter.filterFiles(new File[] {file}).size());
		file.setLastModified(file.lastModified() + 5000L);
		assertEquals(1, filter.filterFiles(new File[] {file}).size());
		ts = store.get("foo:" + file.getAbsolutePath());
		assertEquals(String.valueOf(file.lastModified()), ts);
		assertEquals(0, filter.filterFiles(new File[] {file}).size());

		suspend.set(true);
		file.setLastModified(file.lastModified() + 5000L);

		Future<Integer> result = Executors.newSingleThreadExecutor().submit(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				return filter.filterFiles(new File[] {file}).size();
			}
		});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		store.put("foo:" + file.getAbsolutePath(), "43");
		latch1.countDown();
		Integer theResult = result.get(10, TimeUnit.SECONDS);
		assertEquals(Integer.valueOf(0), theResult); // lost the race, key changed

		file.delete();
	}

}
