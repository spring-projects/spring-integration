/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.jdbc.postgres;

import java.time.Duration;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gabriel Hall
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class PostgresLockRepositoryTests implements PostgresContainerTest {

	@Autowired
	DataSource dataSource;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Test
	void contendedAcquireDoesNotFailOnPrimaryKey() {
		DefaultLockRepository holder = lockRepository();
		DefaultLockRepository contender = lockRepository();

		assertThat(holder.acquire("contended", Duration.ofSeconds(10))).isTrue();
		assertThat(contender.acquire("contended", Duration.ofSeconds(10))).isFalse();
		holder.delete("contended");

		assertThat(POSTGRES_CONTAINER.getLogs()).doesNotContain("int_lock_pk");
	}

	private DefaultLockRepository lockRepository() {
		DefaultLockRepository lockRepository = new DefaultLockRepository(this.dataSource);
		lockRepository.setTransactionManager(this.transactionManager);
		lockRepository.afterPropertiesSet();
		lockRepository.afterSingletonsInstantiated();
		return lockRepository;
	}

	@Configuration(proxyBeanMethods = false)
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

	}

}
