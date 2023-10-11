/*
 * Copyright 2002-2023 the original author or authors.
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

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jdbc.channel.PostgresContainerTest;
import org.springframework.integration.jdbc.store.PostgresJdbcChannelMessageStore;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * @author Johannes Edmeier
 */
@ContextConfiguration
public class PostgresJdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests implements PostgresContainerTest {
	@BeforeEach
	@Override
	public void init() {
		messageStore = new PostgresJdbcChannelMessageStore(dataSource);
		messageStore.setRegion(REGION);
		messageStore.setChannelMessageStoreQueryProvider(queryProvider);
		messageStore.afterPropertiesSet();
		messageStore.removeMessageGroup("AbstractJdbcChannelMessageStoreTests");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public DataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(PostgresContainerTest.getJdbcUrl());
			dataSource.setUsername(PostgresContainerTest.getUsername());
			dataSource.setPassword(PostgresContainerTest.getPassword());
			return dataSource;
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			ResourceDatabasePopulator databasePopulator =
					new ResourceDatabasePopulator(new ClassPathResource("org/springframework/integration/jdbc/schema-drop-postgresql.sql"),
							new ClassPathResource("org/springframework/integration/jdbc/schema-postgresql.sql"));
			databasePopulator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
			dataSourceInitializer.setDatabasePopulator(
					databasePopulator);
			return dataSourceInitializer;
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		DeleteReturningPostgresChannelMessageStoreQueryProvider queryProvider() {
			return new DeleteReturningPostgresChannelMessageStoreQueryProvider();
		}
	}
}
