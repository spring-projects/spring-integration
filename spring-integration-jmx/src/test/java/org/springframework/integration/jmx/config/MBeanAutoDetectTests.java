/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MBeanAutoDetectTests {

	private MBeanServer server;

	private ClassPathXmlApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testRouterMBeanExistsWhenDefinedFirst() throws Exception {
		context = new ClassPathXmlApplicationContext("MBeanAutoDetectFirstTests-context.xml", getClass());
		server = context.getBean(MBeanServer.class);
		// System . err.println(server.queryNames(new ObjectName("test.MBeanAutoDetectFirst:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.MBeanAutoDetectFirst:type=ExpressionEvaluatingRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@Test
	public void testRouterMBeanExistsWhenDefinedSecond() throws Exception {
		context = new ClassPathXmlApplicationContext("MBeanAutoDetectSecondTests-context.xml", getClass());
		server = context.getBean(MBeanServer.class);
		// System . err.println(server.queryNames(new ObjectName("test.MBeanAutoDetectFirst:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.MBeanAutoDetectFirst:type=ExpressionEvaluatingRouter,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

}
