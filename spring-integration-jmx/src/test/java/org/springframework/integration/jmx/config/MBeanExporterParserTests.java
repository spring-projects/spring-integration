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

import static org.junit.Assert.assertEquals;

import javax.management.MBeanServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.control.ControlBus;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MBeanExporterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void test() throws InterruptedException {
		ControlBus controlBus = this.context.getBean(ControlBus.class);
		assertEquals(controlBus.getOperationChannel(), this.context.getBean("testChannel"));
		MBeanServer server = this.context.getBean("mbs", MBeanServer.class);
		MBeanExporter exporter = (MBeanExporter) new DirectFieldAccessor(controlBus).getPropertyValue("exporter");
		assertEquals(server, exporter.getServer());
		exporter.destroy();
	}

}
