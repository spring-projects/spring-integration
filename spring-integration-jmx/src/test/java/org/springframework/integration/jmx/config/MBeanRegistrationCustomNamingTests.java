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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertEquals(6, names.size());
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler,name=chain,bean=endpoint")));
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler,name=chain$child.t1,bean=handler")));
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=Integration,componentType=MessageHandler,name=chain$child.f1,bean=handler")));
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
