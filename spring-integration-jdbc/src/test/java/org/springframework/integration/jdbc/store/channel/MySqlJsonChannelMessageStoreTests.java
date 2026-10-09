/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.jdbc.mysql.MySqlContainerTest;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Yoobin Yoon
 * @author Artem Bilan
 *
 * @since 7.0
 */
@ContextConfiguration
class MySqlJsonChannelMessageStoreTests extends AbstractJsonChannelMessageStoreTests implements MySqlContainerTest {

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		DataSource dataSource() {
			return MySqlContainerTest.dataSource();
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		MySqlChannelMessageStoreQueryProvider queryProvider() {
			return new MySqlChannelMessageStoreQueryProvider();
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(@Value("schema-mysql-json.sql") Resource createSchemaScript,
				DataSource dataSource) {

			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(createSchemaScript));
			return dataSourceInitializer;
		}

	}

}

