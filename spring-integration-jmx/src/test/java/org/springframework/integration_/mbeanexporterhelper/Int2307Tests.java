/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.Set;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.export.MBeanExporter;

import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class Int2307Tests {

	@Test
	public void testInt2307_DefaultMBeanExporter() throws Exception{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("single-config.xml", this.getClass());
		context.destroy();
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
		context.destroy();
	}
	
	public static class Foo{}
}
