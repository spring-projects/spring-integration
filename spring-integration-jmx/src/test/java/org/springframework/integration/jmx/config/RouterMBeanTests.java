/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * @since 2.0
 */
@RunWith(Parameterized.class)
public class RouterMBeanTests {

	private MBeanServer server;

	private ClassPathXmlApplicationContext context;

	public RouterMBeanTests(String configLocation) {
		context = new ClassPathXmlApplicationContext(configLocation, getClass());
		server = context.getBean(MBeanServer.class);
	}

	@Parameters
	public static List<Object[]> getParameters() {
		return Arrays.asList(
				new Object[] { RouterMBeanTests.class.getSimpleName() + "-context.xml" },
				new Object[] { RouterMBeanTests.class.getSimpleName() + "None-context.xml" },
				new Object[] { RouterMBeanTests.class.getSimpleName() + "Switch-context.xml" });
	}

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testRouterMBeanExists() throws Exception {
		// System.err.println(server.queryNames(new ObjectName("test.RouterMBean:*"), null));
		Set<ObjectName> names = server.queryNames(
				new ObjectName("test.RouterMBean:type=MessageHandler,name=ptRouter,*"), null);
		assertEquals(1, names.size());
	}

	@Test
	public void testRouterMBeanOnlyRegisteredOnce() throws Exception {
		// System.err.println(server.queryNames(new ObjectName("*:type=MessageHandler,*"), null));
		// The errorLogger and the router
		assertEquals(2, server.queryNames(new ObjectName("test.RouterMBean:type=MessageHandler,*"), null).size());
	}

}
