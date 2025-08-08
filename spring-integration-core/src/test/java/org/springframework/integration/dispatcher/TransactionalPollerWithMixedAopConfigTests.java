/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
