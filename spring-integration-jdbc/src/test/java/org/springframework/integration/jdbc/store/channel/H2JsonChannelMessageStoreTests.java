/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Yoobin Yoon
 *
 * @since 7.0
 */
@ContextConfiguration
class H2JsonChannelMessageStoreTests extends AbstractJsonChannelMessageStoreTests {

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.H2)
					.addScript("classpath:schema-h2-json.sql")
					.build();
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		H2ChannelMessageStoreQueryProvider queryProvider() {
			return new H2ChannelMessageStoreQueryProvider();
		}

	}

}
