/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Bojan Vukasovic
 * @author Artem Vozhdayenko
 *
 * @since 4.0
 */
public class PersistentAcceptOnceFileListFilterExternalStoreTests implements RedisContainerTest {

	static RedisConnectionFactory redisConnectionFactory;

	@BeforeAll
	static void setupConnectionFactory() {
		redisConnectionFactory = RedisContainerTest.connectionFactory();
	}

	@Test
	public void testFileSystemWithRedisMetadataStore() throws Exception {
		RedisTemplate<String, ?> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		template.delete("persistentAcceptOnceFileListFilterRedisTests");

		try {
			this.testFileSystem(new RedisMetadataStore(redisConnectionFactory,
					"persistentAcceptOnceFileListFilterRedisTests"));
		}
		finally {
			template.delete("persistentAcceptOnceFileListFilterRedisTests");
		}
	}

	@Test
	public void testFileSystemWithJdbcMetadataStore() throws Exception {
		EmbeddedDatabase dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-drop-h2.sql")
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();

		JdbcMetadataStore metadataStore = new JdbcMetadataStore(dataSource);
		metadataStore.setLockHint("");
		metadataStore.afterPropertiesSet();

		try {
			testFileSystem(metadataStore);

			List<Map<String, Object>> metaData = new JdbcTemplate(dataSource)
					.queryForList("SELECT * FROM INT_METADATA_STORE");

			assertThat(metaData).hasSize(1);
			assertThat(metaData.get(0)).containsEntry("METADATA_VALUE", "43");
		}
		finally {
			dataSource.shutdown();
		}
	}

	private void testFileSystem(ConcurrentMetadataStore store) throws Exception {
		final AtomicBoolean suspend = new AtomicBoolean();

		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);

		store = Mockito.spy(store);

		Mockito.doAnswer(invocation -> {
			if (suspend.get()) {
				latch2.countDown();
				try {
					latch1.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return invocation.callRealMethod();
		}).when(store).replace(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

		final FileSystemPersistentAcceptOnceFileListFilter filter =
				new FileSystemPersistentAcceptOnceFileListFilter(store, "foo:");
		final File file = File.createTempFile("foo", ".txt");
		assertThat(filter.filterFiles(new File[] {file})).hasSize(1);
		String ts = store.get("foo:" + file.getAbsolutePath());
		assertThat(ts).isEqualTo(String.valueOf(file.lastModified()));
		assertThat(filter.filterFiles(new File[] {file})).isEmpty();
		assertThat(file.setLastModified(file.lastModified() + 5000L)).isTrue();
		assertThat(filter.filterFiles(new File[] {file})).hasSize(1);
		ts = store.get("foo:" + file.getAbsolutePath());
		assertThat(ts).isEqualTo(String.valueOf(file.lastModified()));
		assertThat(filter.filterFiles(new File[] {file})).isEmpty();

		suspend.set(true);
		assertThat(file.setLastModified(file.lastModified() + 5000L)).isTrue();

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Integer> result = executorService
				.submit(() -> filter.filterFiles(new File[] {file}).size());
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		store.put("foo:" + file.getAbsolutePath(), "43");
		latch1.countDown();
		Integer theResult = result.get(10, TimeUnit.SECONDS);
		assertThat(theResult).isEqualTo(Integer.valueOf(0)); // lost the race, key changed

		assertThat(file.delete()).isTrue();
		filter.close();
		executorService.shutdown();
	}

}
