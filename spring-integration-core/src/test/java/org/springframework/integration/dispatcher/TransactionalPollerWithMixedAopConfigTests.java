/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test was influenced by INT-1483 where via registering TX Advisor
 * in the BeanFactory while having <aop:config> resent resulted in
 * TX Advisor being applied on all beans in AC
 *
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class TransactionalPollerWithMixedAopConfigTests {

	@Autowired
	ApplicationContext applicationContext;

	@Test
	public void validateTransactionalProxyIsolationToThePollerOnly() {
		assertThat(this.applicationContext.getBean("foo")).isNotInstanceOf(Advised.class);
		assertThat(applicationContext.getBean("inputChannel")).isNotInstanceOf(Advised.class);
	}

	public static class SampleService {

		public void foo(String payload) {
		}

	}

	public static class Foo {

		public Foo(String value) {
		}

	}

}
