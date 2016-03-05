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

package org.springframework.integration_.mbeanexporterhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.export.MBeanExporter;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class Int2307Tests {

	@SuppressWarnings("unchecked")
	@Test
	public void testInt2307_DefaultMBeanExporter() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("single-config.xml", getClass());
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertEquals(1, servers.size());
		MBeanServer server = servers.get(0);
		Set<ObjectInstance> mbeans = server.queryMBeans(null, null);
		int bits = 0;
		int count = 0;
		for (ObjectInstance mbean : mbeans) {
			if (mbean.toString()
					.startsWith("org.springframework.integration.support.management.LifecycleTrackableMessageHandlerMetrics[test.domain:type=MessageHandler,name=rlr,bean=endpoint,random=")) {
				bits |= 2;
				count++;
			}
			else if (mbean.toString()
					.startsWith("org.springframework.integration.support.management.TrackableRouterMetrics[test.domain:type=MessageHandler,name=hvr,bean=endpoint,random=")) {
				bits |= 8;
				count++;
			}
		}
		assertEquals(0xa, bits);
		assertEquals(2, count);

		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors =
				TestUtils.getPropertyValue(context, "beanFactory.beanPostProcessors", List.class);
		Object mBeanExporterHelper = null;
		for (Object beanPostProcessor : beanPostProcessors) {
			if (clazz.isAssignableFrom(beanPostProcessor.getClass())) {
				mBeanExporterHelper = beanPostProcessor;
				break;
			}
		}
		assertNotNull(mBeanExporterHelper);
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("z"));
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("zz"));
		context.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInt2307_CustomMBeanExporter() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("single-config-custom-exporter.xml", getClass());
		MBeanExporter exporter = context.getBean("myExporter", MBeanExporter.class);
		Set<String> excludedBeanNames = TestUtils.getPropertyValue(exporter, "excludedBeans", Set.class);
		assertTrue(excludedBeanNames.contains("x"));
		assertTrue(excludedBeanNames.contains("y"));
		assertTrue(excludedBeanNames.contains("foo")); // non SI bean
		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors =
				TestUtils.getPropertyValue(context, "beanFactory.beanPostProcessors", List.class);
		Object mBeanExporterHelper = null;
		for (Object beanPostProcessor : beanPostProcessors) {
			if (clazz.isAssignableFrom(beanPostProcessor.getClass())) {
				mBeanExporterHelper = beanPostProcessor;
				break;
			}
		}
		assertNotNull(mBeanExporterHelper);
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("z"));
		context.close();
	}

	public static class Foo {

	}

}
