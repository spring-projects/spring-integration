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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@ContextConfiguration(initializers = EnableMBeanExportTests.EnvironmentApplicationContextInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class EnableMBeanExportTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private IntegrationMBeanExporter exporter;

	@Autowired
	private MBeanServer mBeanServer;

	@SuppressWarnings("unchecked")
	@Test
	public void testEnableMBeanExport() throws MalformedObjectNameException, ClassNotFoundException {
		assertTrue(AopUtils.isAopProxy(this.beanFactory.getBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)));
		assertTrue(AopUtils.isAopProxy(this.beanFactory.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)));

		assertSame(this.mBeanServer, this.exporter.getServer());
		String[] componentNamePatterns = TestUtils.getPropertyValue(this.exporter, "componentNamePatterns", String[].class);
		for (String componentNamePattern : componentNamePatterns) {
			assertThat(componentNamePattern, Matchers.isOneOf("input", "in*"));
			assertThat(componentNamePattern, Matchers.not(Matchers.equalTo("*")));
		}
		Set<ObjectName> names = this.mBeanServer.queryNames(ObjectName.getInstance("FOO:type=MessageChannel,*"), null);
		// Only one registered (out of >2 available)
		assertEquals(1, names.size());
		assertEquals("input", names.iterator().next().getKeyProperty("name"));
		names = this.mBeanServer.queryNames(ObjectName.getInstance("FOO:type=MessageHandler,*"), null);
		assertEquals(0, names.size());

		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors = TestUtils.getPropertyValue(beanFactory, "beanPostProcessors", List.class);
		Object mBeanExporterHelper = null;
		for (Object beanPostProcessor : beanPostProcessors) {
			if (clazz.isAssignableFrom(beanPostProcessor.getClass())) {
				mBeanExporterHelper = beanPostProcessor;
				break;
			}
		}
		assertNotNull(mBeanExporterHelper);
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("input"));
		assertTrue(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("output"));
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "#{mbeanServer}", defaultDomain = "${managed.domain}", managedComponents = {"input", "${managed.component}"})
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

	public static class EnvironmentApplicationContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			applicationContext.setEnvironment(new MockEnvironment()
					.withProperty("managed.component", "input,in*")
					.withProperty("managed.domain", "FOO"));
		}

	}

}
