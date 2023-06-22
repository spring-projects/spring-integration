/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.file.tail.OSDelegatingFileTailingMessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gavin Gray
 * @author Ali Shahbour
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FileTailInboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("default")
	private OSDelegatingFileTailingMessageProducer defaultAdapter;

	@Autowired
	@Qualifier("native")
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
	private MessageChannel tailErrorChannel;

	@Test
	public void testDefault() {
		String fileName = TestUtils.getPropertyValue(defaultAdapter, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertThat(normalizedName).isEqualTo("/tmp/baz");
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "command")).isEqualTo("tail -F -n 0 " + fileName);
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "taskExecutor")).isSameAs(exec);
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "autoStartup", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "enableStatusReader", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "phase")).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "errorChannel")).isSameAs(this.tailErrorChannel);
		this.defaultAdapter.stop();
		this.defaultAdapter.setOptions("-F -n 6");
		this.defaultAdapter.start();
		assertThat(TestUtils.getPropertyValue(defaultAdapter, "command")).isEqualTo("tail -F -n 6 " + fileName);
	}

	@Test
	public void testNative() {
		String fileName = TestUtils.getPropertyValue(nativeAdapter, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertThat(normalizedName).isEqualTo("/tmp/foo");
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "command")).isEqualTo("tail -F -n 6 " + fileName);
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "taskExecutor")).isSameAs(exec);
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "taskScheduler")).isSameAs(sched);
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "autoStartup", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "enableStatusReader", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "phase")).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(nativeAdapter, "tailAttemptsDelay")).isEqualTo(456L);
	}

	@Test
	public void testApacheDefault() {
		String fileName = TestUtils.getPropertyValue(apacheDefault, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertThat(normalizedName).isEqualTo("/tmp/bar");
		assertThat(TestUtils.getPropertyValue(apacheDefault, "taskExecutor")).isSameAs(exec);
		assertThat(TestUtils.getPropertyValue(apacheDefault, "pollingDelay")).isEqualTo(Duration.ofSeconds(2));
		assertThat(TestUtils.getPropertyValue(apacheDefault, "tailAttemptsDelay")).isEqualTo(10000L);
		assertThat(TestUtils.getPropertyValue(apacheDefault, "idleEventInterval")).isEqualTo(10000L);
		assertThat(TestUtils.getPropertyValue(apacheDefault, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(apacheDefault, "phase")).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(apacheDefault, "end")).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.getPropertyValue(apacheDefault, "reopen")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void testApacheEndReopen() {
		String fileName = TestUtils.getPropertyValue(apacheEndReopen, "file", File.class).getAbsolutePath();
		String normalizedName = getNormalizedPath(fileName);
		assertThat(normalizedName).isEqualTo("/tmp/qux");
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "taskExecutor")).isSameAs(exec);
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "pollingDelay")).isEqualTo(Duration.ofSeconds(2));
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "tailAttemptsDelay")).isEqualTo(10000L);
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "phase")).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "end")).isEqualTo(Boolean.FALSE);
		assertThat(TestUtils.getPropertyValue(apacheEndReopen, "reopen")).isEqualTo(Boolean.TRUE);
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

	public static class Config {

		@Bean
		public TaskExecutor exec() {
			return mock(TaskExecutor.class);
		}

	}

}
