/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
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
