/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.jmx.inbound;

import javax.management.MBeanServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.jmx.support.MBeanServerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 */
public class MBeanTreePollingMessageSourceTests {

	private static MBeanServerFactoryBean factoryBean;

	private static MBeanServer server;

	@BeforeAll
	public static void setup() {
		factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		factoryBean.afterPropertiesSet();
		server = factoryBean.getObject();
	}

	@AfterAll
	public static void tearDown() {
		factoryBean.destroy();
	}

	@Test
	public void testDefaultPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);

		Object received = source.doReceive();

		assertThat(received)
				.asInstanceOf(map(String.class, Object.class))
				.containsKeys("java.lang:type=OperatingSystem",
						"java.lang:type=Runtime",
						"java.util.logging:type=Logging");
	}

	@Test
	public void testQueryNameFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryName("java.lang:*");

		Object received = source.doReceive();

		assertThat(received)
				.asInstanceOf(map(String.class, Object.class))
				.containsKeys("java.lang:type=OperatingSystem", "java.lang:type=Runtime")
				.doesNotContainKey("java.util.logging:type=Logging");
	}

	@Test
	public void testQueryExpressionFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryExpression("*:type=Logging");

		Object received = source.doReceive();

		assertThat(received)
				.asInstanceOf(map(String.class, Object.class))
				.doesNotContainKeys("java.lang:type=OperatingSystem", "java.lang:type=Runtime")
				.containsKeys("java.util.logging:type=Logging");
	}

}
