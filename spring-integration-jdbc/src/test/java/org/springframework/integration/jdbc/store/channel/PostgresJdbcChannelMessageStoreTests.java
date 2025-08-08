/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.postgres.PostgresContainerTest;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Johannes Edmeier
 * @author Artem Bilan
 *
 * @since 6.2
 */
@ContextConfiguration
public class PostgresJdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests
		implements PostgresContainerTest {

	@Configuration
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
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		PostgresChannelMessageStoreQueryProvider queryProvider() {
			return new PostgresChannelMessageStoreQueryProvider();
		}

	}

}
