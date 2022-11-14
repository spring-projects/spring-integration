/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.integration.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.jmx.support.MBeanServerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 */
public class MBeanTreePollingMessageSourceTests {

	private static MBeanServerFactoryBean factoryBean;

	private static MBeanServer server;

	@BeforeClass
	public static void setup() {
		factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		factoryBean.afterPropertiesSet();
		server = factoryBean.getObject();
	}

	@AfterClass
	public static void tearDown() {
		factoryBean.destroy();
	}

	@Test
	public void testDefaultPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);

		Object received = source.doReceive();

		assertThat(received.getClass()).isEqualTo(HashMap.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a couple of MBeans
		assertThat(beans.containsKey("java.lang:type=OperatingSystem")).isTrue();
		assertThat(beans.containsKey("java.lang:type=Runtime")).isTrue();
	}

	@Test
	public void testQueryNameFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryName("java.lang:*");

		Object received = source.doReceive();

		assertThat(received.getClass()).isEqualTo(HashMap.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a few MBeans
		assertThat(beans.containsKey("java.lang:type=OperatingSystem")).isTrue();
		assertThat(beans.containsKey("java.lang:type=Runtime")).isTrue();
		assertThat(beans.containsKey("java.util.logging:type=Logging")).isFalse();
	}

	@Test
	public void testQueryExpressionFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryExpression("*:type=Logging");

		Object received = source.doReceive();

		assertThat(received.getClass()).isEqualTo(HashMap.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a few MBeans
		assertThat(beans.containsKey("java.lang:type=OperatingSystem")).isFalse();
		assertThat(beans.containsKey("java.lang:type=Runtime")).isFalse();
		assertThat(beans.containsKey("java.util.logging:type=Logging")).isTrue();
	}

}
