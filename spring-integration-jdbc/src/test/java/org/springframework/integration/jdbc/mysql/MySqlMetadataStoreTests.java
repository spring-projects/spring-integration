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

package org.springframework.integration.jdbc.mysql;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.4
 */
@SpringJUnitConfig
@DirtiesContext
class MySqlMetadataStoreTests implements MySqlContainerTest {

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

		@Value("org/springframework/integration/jdbc/schema-mysql.sql")
		Resource createSchemaScript;

		@Bean
		DataSource dataSource() {
			return MySqlContainerTest.dataSource();
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(this.createSchemaScript));
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
