/*
 * Copyright 2013-present the original author or authors.
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
