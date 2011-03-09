/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.Properties;

import javax.management.MBeanServer;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MBeanExporterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMBeanExporterExists() throws InterruptedException {
		IntegrationMBeanExporter exporter = this.context.getBean(IntegrationMBeanExporter.class);
		MBeanServer server = this.context.getBean("mbs", MBeanServer.class);
		Properties properties = TestUtils.getPropertyValue(exporter, "objectNameStaticProperties", Properties.class);
		assertNotNull(properties);
		assertEquals(2, properties.size());
		assertTrue(properties.containsKey("foo"));
		assertTrue(properties.containsKey("bar"));
		assertEquals(server, exporter.getServer());
		exporter.destroy();
	}

}
