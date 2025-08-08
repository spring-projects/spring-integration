/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.naming.KeyNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MBeanRegistrationCustomNamingTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler,*"), null);
		assertThat(names.size()).isEqualTo(6);
		assertThat(names
				.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler," +
						"name=chain,bean=endpoint")))
				.isTrue();
		assertThat(names
				.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler," +
						"name=chain$child.t1,bean=handler")))
				.isTrue();
		assertThat(names
				.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler,name=chain$child.f1,bean=handler")))
				.isTrue();
	}

	public static class Source {

		public String get() {
			return "foo";
		}

	}

	public static class Namer implements ObjectNamingStrategy {

		private final ObjectNamingStrategy realNamer = new KeyNamingStrategy();

		@Override
		public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
			String actualBeanKey = beanKey.replace("type=", "type=Integration,componentType=");
			return realNamer.getObjectName(managedBean, actualBeanKey);
		}

	}

}
