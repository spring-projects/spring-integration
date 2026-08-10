/*
 * Copyright 2024-present the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Sanghun Lee
 *
 * @since 6.4
 */
@SpringJUnitConfig
@DirtiesContext
class MySqlMetadataStoreTests implements MySqlContainerTest {

	@Autowired
	ConcurrentMetadataStore jdbcMetadataStore;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	DataSource dataSource;

	@Test
	void verifyJdbcMetadataStoreConcurrency() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		CountDownLatch successPutIfAbsents = new CountDownLatch(100);
		for (int i = 0; i < 100; i++) {
			executorService.execute(() -> {
				this.jdbcMetadataStore.putIfAbsent("testKey", "testValue");
				successPutIfAbsents.countDown();
			});
		}
		assertThat(successPutIfAbsents.await(10, TimeUnit.SECONDS)).isTrue();
		executorService.shutdown();
	}

	@Test
	void verifyPutIfAbsentSeesConcurrentlyCommittedValue() throws Exception {
		String key = "concurrentlyCommittedKey";
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		// The scenario only exists under a snapshot-based isolation level, so pin it here rather
		// than depending on the server default staying REPEATABLE READ.
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);

		CountDownLatch snapshotTaken = new CountDownLatch(1);
		CountDownLatch otherTransactionCommitted = new CountDownLatch(1);

		// Bound the worker transaction so a regression times out instead of looping with its locks
		// held, which would leave the cleanup below blocking rather than the assertion reporting.
		TransactionTemplate workerTransactionTemplate = new TransactionTemplate(this.transactionManager);
		workerTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		workerTransactionTemplate.setTimeout(30);

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		try {
			Future<String> putIfAbsentResult = executorService.submit(() ->
					workerTransactionTemplate.execute(status -> {
						// Establish this transaction's snapshot before the other one inserts
						// the key, like a channel adapter reading before it records metadata.
						jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INT_METADATA_STORE", Integer.class);
						snapshotTaken.countDown();
						try {
							assertThat(otherTransactionCommitted.await(10, TimeUnit.SECONDS)).isTrue();
						}
						catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
							throw new IllegalStateException(ex);
						}
						return this.jdbcMetadataStore.putIfAbsent(key, "thisValue");
					}));

			assertThat(snapshotTaken.await(10, TimeUnit.SECONDS)).isTrue();
			transactionTemplate.executeWithoutResult(status ->
					jdbcTemplate.update("INSERT INTO INT_METADATA_STORE(METADATA_KEY, METADATA_VALUE, REGION) "
							+ "VALUES (?, ?, ?)", key, "otherValue", "DEFAULT"));
			otherTransactionCommitted.countDown();

			assertThat(putIfAbsentResult.get(10, TimeUnit.SECONDS)).isEqualTo("otherValue");
		}
		finally {
			executorService.shutdownNow();
			executorService.awaitTermination(10, TimeUnit.SECONDS);
			this.jdbcMetadataStore.remove(key);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class TestConfiguration {

		@Value("org/springframework/integration/jdbc/schema-mysql.sql")
		Resource createSchemaScript;

		@Value("org/springframework/integration/jdbc/schema-drop-mysql.sql")
		Resource dropSchemaScript;

		@Bean
		DataSource dataSource() {
			return MySqlContainerTest.dataSource();
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(this.dropSchemaScript, this.createSchemaScript));
			return dataSourceInitializer;
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		JdbcMetadataStore jdbcMetadataStore(DataSource dataSource) {
			return new JdbcMetadataStore(dataSource);
		}

	}

}
