/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MBeanRegistrationTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MBeanRegistration:type=MessageHandler,*"), null);
		assertEquals(6, names.size());
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=MessageHandler,name=chain,bean=endpoint")));
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=MessageHandler,name=chain$child.t1,bean=handler")));
		assertTrue(names.contains(new ObjectName("test.MBeanRegistration:type=MessageHandler,name=chain$child.f1,bean=handler")));
	}

	@Test
	public void testExporterMBeanRegistration() throws Exception {
		// System . err.println(server.queryNames(new ObjectName("*:type=*MBeanExporter,*"), null));
		// System . err.println(Arrays.asList(server.getMBeanInfo(server.queryNames(new ObjectName("*:type=*Handler,*"), null).iterator().next()).getAttributes()));
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MBeanRegistration:type=IntegrationMBeanExporter,name=integrationMbeanExporter,*"), null);
		assertEquals(1, names.size());
		names = server.queryNames(new ObjectName("test.MBeanRegistration:*,name=org.springframework.integration.MyGateway"), null);
		assertEquals(server.toString(), 1, names.size());
	}

	@Test
	@Ignore // re-instate this if Spring decides to look for @ManagedResource on super classes
	public void testServiceActivatorMBeanHasTrackableComponent() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MBeanRegistration:type=ServiceActivatingHandler,name=service,*"), null);
		Map<String, MBeanOperationInfo> infos = new HashMap<String, MBeanOperationInfo>();
		for (MBeanOperationInfo info : server.getMBeanInfo(names.iterator().next()).getOperations()) {
			infos.put(info.getName(), info);
		}
		assertNotNull(infos.get("setShouldTrack"));
	}

	public static class Source {
		public String get() {
			return "foo";
		}
	}

	public static class MyMessagingGateway extends MessagingGatewaySupport {

	}

}
