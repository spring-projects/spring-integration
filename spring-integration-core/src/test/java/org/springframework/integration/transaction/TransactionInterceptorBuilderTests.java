/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.transaction;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class TransactionInterceptorBuilderTests {

	@Autowired
	private PlatformTransactionManager txm;

	@Autowired
	private TransactionInterceptor interceptor1;

	@Autowired
	private TransactionInterceptor interceptor2;

	@Test
	public void test() throws Throwable {
		verify(this.interceptor1, this.txm);
		verify(this.interceptor2, null);
	}

	private void verify(TransactionInterceptor interceptor, PlatformTransactionManager txm) throws Exception {
		assertThat(interceptor.getTransactionManager()).isSameAs(txm);
		TransactionAttribute atts = interceptor.getTransactionAttributeSource()
				.getTransactionAttribute(TransactionInterceptorBuilderTests.class.getDeclaredMethod("test"), null);
		assertThat(atts.getPropagationBehavior()).isEqualTo(Propagation.REQUIRES_NEW.value());
		assertThat(atts.getIsolationLevel()).isEqualTo(Isolation.SERIALIZABLE.value());
		assertThat(atts.getTimeout()).isEqualTo(42);
		assertThat(atts.isReadOnly()).isTrue();
	}

	@Configuration
	public static class Config {

		@Bean
		public PseudoTransactionManager transactionManager() {
			return new PseudoTransactionManager();
		}

		@Bean
		public TransactionInterceptor interceptor1(TransactionManager transactionManager) {
			return new TransactionInterceptorBuilder()
					.propagation(Propagation.REQUIRES_NEW)
					.isolation(Isolation.SERIALIZABLE)
					.timeout(42)
					.readOnly(true)
					.transactionManager(transactionManager)
					.build();
		}

		@Bean
		public TransactionInterceptor interceptor2() {
			return new TransactionInterceptorBuilder()
					.propagation(Propagation.REQUIRES_NEW)
					.isolation(Isolation.SERIALIZABLE)
					.timeout(42)
					.readOnly(true)
					.build();
		}

	}

}
