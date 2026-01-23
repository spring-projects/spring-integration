/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.resource;

import java.io.File;
import java.util.Collection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.CollectionFilter;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.1
 */
public class ResourceInboundChannelAdapterParserTests {

	private static File workDir;

	@BeforeAll
	public static void setupClass() {
		workDir = new File(System.getProperty("java.io.tmpdir"), "ResourceInboundChannelAdapterParserTests");
		workDir.mkdir();
		workDir.deleteOnExit();
	}

	@AfterEach
	public void cleanUpWorkDir() throws Exception {
		File[] listFiles = workDir.listFiles();
		for (File file : listFiles) {
			file.delete();
		}
	}

	@AfterAll
	public static void cleanUp() {
		workDir.delete();
	}

	@Test
	public void testDefaultConfig() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ResourcePatternResolver-config.xml", getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault",
				SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source");
		assertThat(source).isNotNull();
		boolean autoStartup = TestUtils.<Boolean>getPropertyValue(resourceAdapter, "autoStartup");
		assertThat(autoStartup).isFalse();

		assertThat(TestUtils.<String>getPropertyValue(source, "pattern")).isEqualTo("/**/*");
		assertThat(TestUtils.<ClassPathXmlApplicationContext>getPropertyValue(source, "patternResolver"))
				.isEqualTo(context);
		context.close();
	}

	@Test
	public void testDefaultConfigNoLocationPattern() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("ResourcePatternResolver-config-fail.xml", getClass()));
	}

	@Test
	public void testCustomPatternResolver() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ResourcePatternResolver-config-custom.xml", getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault",
				SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source");
		assertThat(source).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(source, "patternResolver"))
				.isEqualTo(context.getBean("customResolver"));
		context.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUsage() throws Exception {

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsage" + i);
			f.createNewFile();
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ResourcePatternResolver-config-usage.xml", this.getClass());
		QueueChannel resultChannel = context.getBean("resultChannel", QueueChannel.class);
		Message<Resource[]> message = (Message<Resource[]>) resultChannel.receive(10000);
		assertThat(message).isNotNull();
		Resource[] resources = message.getPayload();
		for (Resource resource : resources) {
			assertThat(resource.getURI().toString().contains("testUsage")).isTrue();
		}
		context.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUsageWithCustomResourceFilter() throws Exception {

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsageWithRf" + i);
			f.createNewFile();
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ResourcePatternResolver-config-usagerf.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault",
				SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source");
		assertThat(source).isNotNull();
		TestCollectionFilter customFilter = context.getBean("customFilter", TestCollectionFilter.class);
		assertThat(TestUtils.<TestCollectionFilter>getPropertyValue(source, "filter"))
				.isEqualTo(customFilter);

		assertThat(customFilter.invoked).isFalse();
		resourceAdapter.start();
		QueueChannel resultChannel = context.getBean("resultChannel", QueueChannel.class);
		Message<Resource[]> message = (Message<Resource[]>) resultChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(customFilter.invoked).isTrue();
		context.close();
	}

	@Test
	public void testUsageWithEmptyFilter() throws Exception {

		for (int i = 0; i < 10; i++) {
			File f = new File(workDir, "testUsageWithRf" + i);
			f.createNewFile();
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ResourcePatternResolver-config-usage-emptyref.xml", this.getClass());
		SourcePollingChannelAdapter resourceAdapter = context.getBean("resourceAdapterDefault",
				SourcePollingChannelAdapter.class);
		ResourceRetrievingMessageSource source = TestUtils.getPropertyValue(resourceAdapter, "source");
		assertThat(source).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(source, "filter")).isNull();
		context.close();
	}

	public static class TestCollectionFilter implements CollectionFilter<Resource> {

		private volatile boolean invoked = false;

		@Override
		public Collection<Resource> filter(Collection<Resource> unfilteredResources) {
			this.invoked = true;
			return unfilteredResources;
		}

	}

}
