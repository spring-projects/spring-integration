/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.jdbc.postgres;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.2.9
 */
@SpringJUnitConfig
@DirtiesContext
class PostgresMetadataStoreTests implements PostgresContainerTest {

	@Autowired
	ConcurrentMetadataStore jdbcMetadataStore;

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

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class TestConfiguration {

		@Bean
		DataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(PostgresContainerTest.getJdbcUrl());
			dataSource.setUsername(PostgresContainerTest.getUsername());
			dataSource.setPassword(PostgresContainerTest.getPassword());
			return dataSource;
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
