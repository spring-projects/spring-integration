/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileTailInboundChannelAdapterParserTests {

	@Autowired @Qualifier("default")
	private OSDelegatingFileTailingMessageProducer defaultAdapter;

	@Autowired @Qualifier("native")
	private OSDelegatingFileTailingMessageProducer nativeAdapter;

	@Autowired
	private ApacheCommonsFileTailingMessageProducer apacheAdapter;

	@Autowired
	private TaskExecutor exec;

	@Test
	public void testDefault() {
		assertEquals("tail -F -n 0 /tmp/baz", TestUtils.getPropertyValue(defaultAdapter, "command"));
		assertEquals("/tmp/baz", TestUtils.getPropertyValue(defaultAdapter, "file", File.class).getAbsolutePath());
		assertSame(exec, TestUtils.getPropertyValue(defaultAdapter, "taskExecutor"));
		assertFalse(TestUtils.getPropertyValue(defaultAdapter, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(defaultAdapter, "phase"));
	}

	@Test
	public void testNative() {
		assertEquals("tail -F -n 6 /tmp/foo", TestUtils.getPropertyValue(nativeAdapter, "command"));
		assertEquals("/tmp/foo", TestUtils.getPropertyValue(nativeAdapter, "file", File.class).getAbsolutePath());
		assertSame(exec, TestUtils.getPropertyValue(nativeAdapter, "taskExecutor"));
		assertFalse(TestUtils.getPropertyValue(nativeAdapter, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(nativeAdapter, "phase"));
	}

	@Test
	public void testApache() {
		assertEquals("/tmp/bar", TestUtils.getPropertyValue(apacheAdapter, "file", File.class).getAbsolutePath());
		assertSame(exec, TestUtils.getPropertyValue(apacheAdapter, "taskExecutor"));
		assertEquals(2000L, TestUtils.getPropertyValue(apacheAdapter, "pollingDelay"));
		assertEquals(10000L, TestUtils.getPropertyValue(apacheAdapter, "missingFileDelay"));
		assertFalse(TestUtils.getPropertyValue(apacheAdapter, "autoStartup", Boolean.class));
		assertEquals(123, TestUtils.getPropertyValue(apacheAdapter, "phase"));
	}

}
