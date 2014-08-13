/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.file.tail.OSDelegatingFileTailingMessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileTailInboundChannelAdapterParserTests {

	@Autowired @Qualifier("default")
	private OSDelegatingFileTailingMessageProducer defaultAdapter;

	@Autowired @Qualifier("native")
	private OSDelegatingFileTailingMessageProducer nativeAdapter;

	@Autowired
	private ApacheCommonsFileTailingMessageProducer apacheDefault;

	@Autowired
	private ApacheCommonsFileTailingMessageProducer apacheEndReopen;

	@Autowired
	private TaskExecutor exec;

	@Autowired
	private TaskScheduler sched;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private MessageChannel tailErrorChannel;

	@Test
	public void testDefault() {
		String fileName = TestUtils.getPropertyValue(defaultAdapter, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertEquals("/tmp/baz", normalizedName);
		assertEquals("tail -F -n 0 " + fileName, TestUtils.getPropertyValue(defaultAdapter, "command"));
		assertSame(exec, TestUtils.getPropertyValue(defaultAdapter, "taskExecutor"));
		assertFalse(TestUtils.getPropertyValue(defaultAdapter, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(defaultAdapter, "phase"));
		assertSame(this.tailErrorChannel, TestUtils.getPropertyValue(defaultAdapter, "errorChannel"));
	}

	@Test
	public void testNative() {
		String fileName = TestUtils.getPropertyValue(nativeAdapter, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertEquals("/tmp/foo", normalizedName);
		assertEquals("tail -F -n 6 " + fileName, TestUtils.getPropertyValue(nativeAdapter, "command"));
		assertSame(exec, TestUtils.getPropertyValue(nativeAdapter, "taskExecutor"));
		assertSame(sched, TestUtils.getPropertyValue(nativeAdapter, "taskScheduler"));
		assertFalse(TestUtils.getPropertyValue(nativeAdapter, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(nativeAdapter, "phase"));
		assertEquals(456L, TestUtils.getPropertyValue(nativeAdapter, "tailAttemptsDelay"));
	}

	@Test
	public void testApacheDefault() {
		String fileName = TestUtils.getPropertyValue(apacheDefault, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertEquals("/tmp/bar", normalizedName);
		assertSame(exec, TestUtils.getPropertyValue(apacheDefault, "taskExecutor"));
		assertEquals(2000L, TestUtils.getPropertyValue(apacheDefault, "pollingDelay"));
		assertEquals(10000L, TestUtils.getPropertyValue(apacheDefault, "tailAttemptsDelay"));
		assertFalse(TestUtils.getPropertyValue(apacheDefault, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(apacheDefault, "phase"));
		assertEquals(Boolean.TRUE, TestUtils.getPropertyValue(apacheDefault, "end"));
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(apacheDefault, "reopen"));
	}

	@Test
	public void testApacheEndReopen() {
		String fileName = TestUtils.getPropertyValue(apacheEndReopen, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertEquals("/tmp/qux", normalizedName);
		assertSame(exec, TestUtils.getPropertyValue(apacheEndReopen, "taskExecutor"));
		assertEquals(2000L, TestUtils.getPropertyValue(apacheEndReopen, "pollingDelay"));
		assertEquals(10000L, TestUtils.getPropertyValue(apacheEndReopen, "tailAttemptsDelay"));
		assertFalse(TestUtils.getPropertyValue(apacheEndReopen, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(apacheEndReopen, "phase"));
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(apacheEndReopen, "end"));
		assertEquals(Boolean.TRUE, TestUtils.getPropertyValue(apacheEndReopen, "reopen"));
	}

	/**
	 * Fix up windows paths.
	 */
	private String getNormalizedPath(String fileName) {
		String absolutePath = fileName.replaceAll("\\\\", "/");
		int index = absolutePath.indexOf(":");
		if (index >= 0) {
			absolutePath = absolutePath.substring(index + 1);
		}
		return absolutePath;
	}
}
