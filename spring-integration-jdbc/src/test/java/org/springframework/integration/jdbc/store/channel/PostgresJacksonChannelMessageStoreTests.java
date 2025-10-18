/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Yoobin Yoon
 * @since 7.0
 */
@ContextConfiguration
public class PostgresJacksonChannelMessageStoreTests extends AbstractJacksonChannelMessageStoreTests {

	private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
			new PostgreSQLContainer<>("postgres:11");

	@BeforeAll
	static void startContainer() {
		POSTGRES_CONTAINER.start();
	}

	@AfterAll
	static void stopContainer() {
		POSTGRES_CONTAINER.stop();
	}

	@Configuration
	public static class Config {

		@Bean
		public DataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(POSTGRES_CONTAINER.getJdbcUrl());
			dataSource.setUsername(POSTGRES_CONTAINER.getUsername());
			dataSource.setPassword(POSTGRES_CONTAINER.getPassword());
			return dataSource;
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public PostgresChannelMessageStoreQueryProvider queryProvider() {
			return new PostgresChannelMessageStoreQueryProvider();
		}

		@Bean
		public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
			DataSourceInitializer initializer = new DataSourceInitializer();
			initializer.setDataSource(dataSource);

			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.setIgnoreFailedDrops(true);
			populator.addScript(new ClassPathResource("org/springframework/integration/jdbc/schema-drop-postgresql.sql"));
			populator.addScript(new ClassPathResource("org/springframework/integration/jdbc/schema-postgres-json.sql"));

			initializer.setDatabasePopulator(populator);
			return initializer;
		}

	}

}
