/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
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
