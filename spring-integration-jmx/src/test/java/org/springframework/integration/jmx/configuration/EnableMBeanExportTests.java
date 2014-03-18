/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.jmx.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class EnableMBeanExportTests {

	@Autowired
	private IntegrationMBeanExporter exporter;

	@Autowired
	private MBeanServer mBeanServer;

	@Test
	public void testEnableMBeanExport() throws MalformedObjectNameException {
		assertSame(this.mBeanServer, this.exporter.getServer());
		Set<ObjectName> names = this.mBeanServer.queryNames(ObjectName.getInstance("org.springframework.integration:type=MessageChannel,*"), null);
		// Only one registered (out of >2 available)
		assertEquals(1, names.size());
		assertEquals("input", names.iterator().next().getKeyProperty("name"));
		names = this.mBeanServer.queryNames(ObjectName.getInstance("org.springframework.integration:type=MessageHandler,*"), null);
		assertEquals(0, names.size());
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "mbeanServer", managedComponents = "input")
	public static class ContextConfiguration {

		@Bean
		public MBeanServerFactoryBean mbeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public QueueChannel input() {
			return new QueueChannel();
		}

		@Bean
		public QueueChannel output() {
			return new QueueChannel();
		}

	}

}
