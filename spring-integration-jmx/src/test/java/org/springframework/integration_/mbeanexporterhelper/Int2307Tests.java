/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration_.mbeanexporterhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
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
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectInstance> mbeans = server.queryMBeans(null, null);
		int bits = 0;
		int count = 0;
		for (ObjectInstance mbean : mbeans) {
			if (mbean.toString()
					.startsWith("org.springframework.integration.router.RecipientListRouter[test.domain:type=MessageHandler,name=rlr,bean=endpoint,random=")) {
				bits |= 2;
				count++;
			}
			else if (mbean.toString()
					.startsWith("org.springframework.integration.router.HeaderValueRouter[test.domain:type=MessageHandler,name=hvr,bean=endpoint,random=")) {
				bits |= 8;
				count++;
			}
		}
		assertThat(bits).isEqualTo(0xa);
		assertThat(count).isEqualTo(2);

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
		assertThat(mBeanExporterHelper).isNotNull();
		assertThat(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("z")).isTrue();
		assertThat(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("zz")).isTrue();
		context.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInt2307_CustomMBeanExporter() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("single-config-custom-exporter.xml", getClass());
		MBeanExporter exporter = context.getBean("myExporter", MBeanExporter.class);
		Set<String> excludedBeanNames = TestUtils.getPropertyValue(exporter, "excludedBeans", Set.class);
		assertThat(excludedBeanNames.contains("x")).isTrue();
		assertThat(excludedBeanNames.contains("y")).isTrue();
		assertThat(excludedBeanNames.contains("foo")).isTrue(); // non SI bean
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
		assertThat(mBeanExporterHelper).isNotNull();
		assertThat(TestUtils.getPropertyValue(mBeanExporterHelper, "siBeanNames", Set.class).contains("z")).isTrue();
		context.close();
	}

	public static class Foo {

	}

}
