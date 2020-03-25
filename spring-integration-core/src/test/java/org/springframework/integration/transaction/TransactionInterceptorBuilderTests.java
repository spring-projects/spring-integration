/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
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
