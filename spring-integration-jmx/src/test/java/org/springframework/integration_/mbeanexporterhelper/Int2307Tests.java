/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration_.mbeanexporterhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.export.MBeanExporter;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class Int2307Tests {

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@SuppressWarnings("unchecked")
	@Test
	public void testInt2307_DefaultMBeanExporter() throws Exception{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("single-config.xml", this.getClass());
		List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
		assertEquals(1, servers.size());
		MBeanServer server = servers.get(0);
		Set<ObjectInstance> mbeans = server.queryMBeans(null, null);
		int bits = 0;
		int count = 0;
		for (ObjectInstance mbean : mbeans) {
			Thread.sleep(500); //Added in order to pass test with Java 8
			if (mbean.toString().startsWith("org.springframework.integration.router.RecipientListRouter[test.domain:type=RecipientListRouter,name=rlr,random=")) {
				bits |= 1;
				count++;
			} else if (mbean.toString().startsWith("org.springframework.integration.monitor.LifecycleMessageHandlerMetrics[test.domain:type=MessageHandler,name=rlr,bean=endpoint,random=")) {
				bits |= 2;
				count++;
			} else if (mbean.toString().startsWith("org.springframework.integration.router.HeaderValueRouter[test.domain:type=HeaderValueRouter,name=hvr,random=")) {
				bits |= 4;
				count++;
			} else if (mbean.toString().startsWith("org.springframework.integration.monitor.LifecycleMessageHandlerMetrics[test.domain:type=MessageHandler,name=hvr,bean=endpoint,random=")) {
				bits |= 8;
				count++;
			}
		}
		assertEquals(0xf, bits);
		assertEquals(4, count);

		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors = TestUtils.getPropertyValue(context, "beanFactory.beanPostProcessors", List.class);
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

		// make sure there are no duplicate MBean ObjectNames if 2 contexts loaded from same config
		new ClassPathXmlApplicationContext("single-config.xml", this.getClass());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInt2307_CustomMBeanExporter() throws Exception{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("single-config-custom-exporter.xml", this.getClass());
		MBeanExporter exporter = context.getBean("myExporter", MBeanExporter.class);
		Set<String> excludedBeanNames = TestUtils.getPropertyValue(exporter, "excludedBeans", Set.class);
		assertTrue(excludedBeanNames.contains("rlr"));
		assertTrue(excludedBeanNames.contains("hvr"));
		assertTrue(excludedBeanNames.contains("x"));
		assertTrue(excludedBeanNames.contains("y"));
		assertTrue(excludedBeanNames.contains("foo")); // non SI bean
		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors = TestUtils.getPropertyValue(context, "beanFactory.beanPostProcessors", List.class);
		Object mBeanExporterHelper = null;
		for (Object beanPostProcessor : beanPostProcessors) {
			if (clazz.isAssignableFrom(beanPostProcessor.getClass())) {
				mBeanExporterHelper = beanPostProcessor;
				break;
			}
		}
		assertNotNull(mBeanExporterHelper);
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("z"));
	}

	public static class Foo{}
}
