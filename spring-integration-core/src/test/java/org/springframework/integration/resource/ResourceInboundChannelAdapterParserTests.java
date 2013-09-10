/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.resource;

import java.io.File;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CollectionFilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class ResourceInboundChannelAdapterParserTests {

	private static File workDir;

	@BeforeClass
	public static void setupClass() {
		workDir = new File(System.getProperty("java.io.tmpdir"), "ResourceInboundChannelAdapterParserTests");
		workDir.mkdir();
		workDir.deleteOnExit();
	}

	@After
	public void cleanUpWorkDir() throws Exception {
		File[] listFiles = workDir.listFiles();
		for (File file : listFiles) {
			file.delete();
		}
	}

	@AfterClass
	public static void cleanUp() {
		workDir.delete();
	}


	@Test
	public void testDefaultConfig(){
		ApplicationContext context = new ClassPathXmlApplicationContext("ResourcePatternResolver-config.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault", SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source", ResourceRetrievingMessageSource.class);
		assertNotNull(source);
		boolean autoStartup = TestUtils.getPropertyValue(resourceAdapter, "autoStartup", Boolean.class);
		assertFalse(autoStartup);

		assertEquals("/**/*", TestUtils.getPropertyValue(source, "pattern"));
		assertEquals(context, TestUtils.getPropertyValue(source, "patternResolver"));
	}

	@Test(expected=BeanCreationException.class)
	public void testDefaultConfigNoLocationPattern(){
		new ClassPathXmlApplicationContext("ResourcePatternResolver-config-fail.xml", this.getClass());
	}

	@Test
	public void testCustomPatternResolver(){
		ApplicationContext context = new ClassPathXmlApplicationContext("ResourcePatternResolver-config-custom.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault", SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source", ResourceRetrievingMessageSource.class);
		assertNotNull(source);
		assertEquals(context.getBean("customResolver"), TestUtils.getPropertyValue(source, "patternResolver"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUsage() throws Exception{

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsage"+i);
			f.createNewFile();
		}

		ApplicationContext context = new ClassPathXmlApplicationContext("ResourcePatternResolver-config-usage.xml", this.getClass());
		QueueChannel resultChannel = context.getBean("resultChannel", QueueChannel.class);
		Message<Resource[]> message = (Message<Resource[]>) resultChannel.receive(3000);
		assertNotNull(message);
		Resource[] resources = message.getPayload();
		for (Resource resource : resources) {
			assertTrue(resource.getURI().toString().contains("testUsage"));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUsageWithCustomResourceFilter() throws Exception{

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsageWithRf"+i);
			f.createNewFile();
		}

		ApplicationContext context = new ClassPathXmlApplicationContext("ResourcePatternResolver-config-usagerf.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault", SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source", ResourceRetrievingMessageSource.class);
		assertNotNull(source);
		TestCollectionFilter customFilter = context.getBean("customFilter", TestCollectionFilter.class);
		assertEquals(customFilter, TestUtils.getPropertyValue(source, "filter"));

		assertFalse(customFilter.invoked);
		resourceAdapter.start();
		QueueChannel resultChannel = context.getBean("resultChannel", QueueChannel.class);
		Message<Resource[]> message = (Message<Resource[]>) resultChannel.receive(1000);
		assertNotNull(message);
		assertTrue(customFilter.invoked);
	}

	@Test
	public void testUsageWithEmptyFilter() throws Exception{

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsageWithRf"+i);
			f.createNewFile();
		}

		ApplicationContext context = new ClassPathXmlApplicationContext("ResourcePatternResolver-config-usage-emptyref.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault", SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source", ResourceRetrievingMessageSource.class);
		assertNotNull(source);
		assertNull(TestUtils.getPropertyValue(source, "filter"));
	}


	public static class TestCollectionFilter implements CollectionFilter<Resource> {

		private volatile boolean invoked = false;

		public Collection<Resource> filter(Collection<Resource> unfilteredResources) {
			this.invoked = true;
			return unfilteredResources;
		}
	}

}
