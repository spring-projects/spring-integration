/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
@SpringJUnitConfig(initializers = EnableMBeanExportTests.EnvironmentApplicationContextInitializer.class)
@DirtiesContext
public class EnableMBeanExportTests {

	@Autowired
	private BeanFactory beanFactory;

	@Autowired
	private IntegrationMBeanExporter exporter;

	@Autowired
	private MBeanServer mBeanServer;

	@Autowired
	private IntegrationManagementConfigurer configurer;

	@SuppressWarnings("unchecked")
	@Test
	public void testEnableMBeanExport() throws MalformedObjectNameException, ClassNotFoundException {
		assertThat(beanFactory.containsBean("jsonPath")).isFalse(); // GH-3541
		assertThat(beanFactory.containsBean("xPath")).isFalse(); // GH-3541

		assertThat(this.exporter.getServer()).isSameAs(this.mBeanServer);
		String[] componentNamePatterns = TestUtils.getPropertyValue(this.exporter, "componentNamePatterns", String[].class);
		assertThat(componentNamePatterns).containsExactly("input", "inputX", "in*");
		assertThat(TestUtils.getPropertyValue(this.configurer, "defaultLoggingEnabled", Boolean.class)).isFalse();

		Set<ObjectName> names = this.mBeanServer.queryNames(ObjectName.getInstance("FOO:type=MessageChannel,*"), null);
		// Only one registered (out of >2 available)
		assertThat(names.size()).isEqualTo(1);
		assertThat(names.iterator().next().getKeyProperty("name")).isEqualTo("input");
		names = this.mBeanServer.queryNames(ObjectName.getInstance("FOO:type=MessageHandler,*"), null);
		assertThat(names.size()).isEqualTo(0);

		Class<?> clazz = Class.forName("org.springframework.integration.jmx.config.MBeanExporterHelper");
		List<Object> beanPostProcessors = TestUtils.getPropertyValue(beanFactory, "beanPostProcessors", List.class);
		Object mBeanExporterHelper = null;
		for (Object beanPostProcessor : beanPostProcessors) {
			if (clazz.isAssignableFrom(beanPostProcessor.getClass())) {
				mBeanExporterHelper = beanPostProcessor;
				break;
			}
		}
		assertThat(mBeanExporterHelper).isNotNull();
		assertThat(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("input"))
				.isTrue();
		assertThat(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("output"))
				.isTrue();
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(
			server = "#{mbeanServer}",
			defaultDomain = "${managed.domain}",
			managedComponents = { "input", "${managed.component}" })
	@EnableIntegrationManagement(defaultLoggingEnabled = "false")
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

	public static class EnvironmentApplicationContextInitializer
			implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			applicationContext.setEnvironment(new MockEnvironment()
					.withProperty("managed.component", "inputX,in*")
					.withProperty("managed.domain", "FOO")
					.withProperty("count.patterns", "bar,baz"));
		}

	}

}
