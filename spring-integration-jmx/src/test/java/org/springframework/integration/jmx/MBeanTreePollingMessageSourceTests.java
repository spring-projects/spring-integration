/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.junit.Before;
import org.junit.Test;

import org.springframework.jmx.support.MBeanServerFactoryBean;

/**
 * @author Stuart Williams
 *
 */
public class MBeanTreePollingMessageSourceTests {

	private MBeanServer server;

	@Before
	public void setup() {
		MBeanServerFactoryBean factoryBean = new MBeanServerFactoryBean();
		factoryBean.setLocateExistingServerIfPossible(true);
		factoryBean.afterPropertiesSet();
		this.server = factoryBean.getObject();
	}

	@Test
	public void testDefaultPoll() {

		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);

		Object received = source.doReceive();

		assertEquals(HashMap.class, received.getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));
	}

	@Test
	public void testQueryNameFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryName("java.lang:*");

		Object received = source.doReceive();

		assertEquals(HashMap.class, received.getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a few MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));
		assertFalse(beans.containsKey("java.util.logging:type=Logging"));
	}

	@Test
	public void testQueryExpressionFilteredPoll() {
		MBeanObjectConverter converter = new DefaultMBeanObjectConverter();
		MBeanTreePollingMessageSource source = new MBeanTreePollingMessageSource(converter);
		source.setServer(server);
		source.setQueryExpression("*:type=Logging");

		Object received = source.doReceive();

		assertEquals(HashMap.class, received.getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) received;

		// test for a few MBeans
		assertFalse(beans.containsKey("java.lang:type=OperatingSystem"));
		assertFalse(beans.containsKey("java.lang:type=Runtime"));
		assertTrue(beans.containsKey("java.util.logging:type=Logging"));
	}

}
