/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.file.tail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport.FileTailingEvent;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport.FileTailingIdleEvent;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Gavin Gray
 * @author Artem Bilan
 * @author Ali Shahbour
 * @author Trung Pham
 *
 * @since 3.0
 */
public class FileTailingMessageProducerTests {

	private static final String TAIL_OPTIONS_FOLLOW_NAME_ALL_LINES = "-F -n +0";

	@Rule
	public TailRule tailRule = new TailRule(TAIL_OPTIONS_FOLLOW_NAME_ALL_LINES);

	private final Log logger = LogFactory.getLog(this.getClass());

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	private File testDir;

	private FileTailingMessageProducerSupport adapter;

	@Before
	public void setup() {
		File f = new File(tmpDir, "FileTailingMessageProducerTests");
		f.mkdir();
		this.testDir = f;
	}

	@After
	public void tearDown() {
		if (this.adapter != null) {
			adapter.stop();
		}
	}

	@Test
	@TailAvailable
	public void testOS() throws Exception {
		OSDelegatingFileTailingMessageProducer adapter = new OSDelegatingFileTailingMessageProducer();
		adapter.setOptions(TAIL_OPTIONS_FOLLOW_NAME_ALL_LINES);
		testGuts(adapter, "stdOutReader");
	}

	@Test
	public void testApache() throws Exception {
		ApacheCommonsFileTailingMessageProducer adapter = new ApacheCommonsFileTailingMessageProducer();
		adapter.setPollingDelay(100);
		adapter.setEnd(false);
		testGuts(adapter, "tailer");
	}

	@Test
	@TailAvailable
	public void canRecalculateCommandWhenFileOrOptionsChanged() throws IOException {
		File firstFile = File.createTempFile("first", ".txt");
		String firstOptions = "-f options";
		File secondFile = File.createTempFile("second", ".txt");
		String secondOptions = "-f newoptions";
		OSDelegatingFileTailingMessageProducer adapter = new OSDelegatingFileTailingMessageProducer();
		adapter.setFile(firstFile);
		adapter.setOptions(firstOptions);

		adapter.setOutputChannel(new QueueChannel());
		adapter.setTailAttemptsDelay(500);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		adapter.start();
		assertThat(adapter.getCommand()).isEqualTo("tail " + firstOptions + " " + firstFile.getAbsolutePath());
		adapter.stop();

		adapter.setFile(secondFile);
		adapter.start();
		assertThat(adapter.getCommand()).isEqualTo("tail " + firstOptions + " " + secondFile.getAbsolutePath());
		adapter.stop();

		adapter.setOptions(secondOptions);
		adapter.start();
		assertThat(adapter.getCommand()).isEqualTo("tail " + secondOptions + " " + secondFile.getAbsolutePath());
		adapter.stop();
	}

	@Test
	public void testIdleEvent() throws Exception {
		ApacheCommonsFileTailingMessageProducer adapter = new ApacheCommonsFileTailingMessageProducer();

		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		adapter.setTaskScheduler(taskScheduler);

		CountDownLatch idleCountDownLatch = new CountDownLatch(1);
		CountDownLatch fileExistCountDownLatch = new CountDownLatch(1);

		adapter.setApplicationEventPublisher(event -> {
			if (event instanceof FileTailingIdleEvent) {
				idleCountDownLatch.countDown();
			}
			if (event instanceof FileTailingEvent) {
				FileTailingEvent fileTailingEvent = (FileTailingEvent) event;
				if (fileTailingEvent.getMessage().contains("File not found")) {
					fileExistCountDownLatch.countDown();
				}
			}
		});

		File file = spy(new File(this.testDir, "foo"));
		file.delete();
		adapter.setFile(file);

		adapter.setOutputChannel(new NullChannel());
		adapter.setIdleEventInterval(10);
		adapter.afterPropertiesSet();
		adapter.start();

		boolean noFile = fileExistCountDownLatch.await(10, TimeUnit.SECONDS);
		assertThat(noFile).as("file does not exist event did not emit ").isTrue();
		boolean noEvent = idleCountDownLatch.await(100, TimeUnit.MILLISECONDS);
		assertThat(noEvent).as("event should not emit when no file exit").isFalse();
		verify(file, atLeastOnce()).exists();

		file.createNewFile();
		boolean eventRaised = idleCountDownLatch.await(10, TimeUnit.SECONDS);
		assertThat(eventRaised).as("idle event did not emit").isTrue();
		adapter.stop();
		file.delete();
	}

	private void testGuts(FileTailingMessageProducerSupport adapter, String field)
			throws Exception {
		this.adapter = adapter;
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		adapter.setTaskScheduler(taskScheduler);
		final List<FileTailingEvent> events = new ArrayList<>();
		adapter.setApplicationEventPublisher(event -> {
			FileTailingEvent tailEvent = (FileTailingEvent) event;
			logger.debug(event);
			events.add(tailEvent);
		});
		adapter.setFile(new File(testDir, "foo"));
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setTailAttemptsDelay(500);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		File file = new File(testDir, "foo");
		File renamed = new File(testDir, "bar");
		file.delete();
		renamed.delete();
		adapter.start();
		waitForField(adapter, field);
		FileOutputStream foo = new FileOutputStream(file);
		for (int i = 0; i < 50; i++) {
			foo.write(("hello" + i + "\n").getBytes());
		}
		foo.flush();
		foo.close();
		for (int i = 0; i < 50; i++) {
			Message<?> message = outputChannel.receive(10000);
			assertThat(message).as("expected a non-null message").isNotNull();
			assertThat(message.getPayload()).isEqualTo("hello" + i);
		}
		file.renameTo(renamed);
		file = new File(testDir, "foo");
		foo = new FileOutputStream(file);
		if (adapter instanceof ApacheCommonsFileTailingMessageProducer) {
			Thread.sleep(1000);
		}
		for (int i = 50; i < 100; i++) {
			foo.write(("hello" + i + "\n").getBytes());
		}
		foo.flush();
		foo.close();
		for (int i = 50; i < 100; i++) {
			Message<?> message = outputChannel.receive(10000);
			assertThat(message).as("expected a non-null message").isNotNull();
			assertThat(message.getPayload()).isEqualTo("hello" + i);
			assertThat(message.getHeaders().get(FileHeaders.ORIGINAL_FILE)).isEqualTo(file);
			assertThat(message.getHeaders().get(FileHeaders.FILENAME)).isEqualTo(file.getName());
		}

		assertThat(events.size()).isGreaterThanOrEqualTo(1);

		taskScheduler.destroy();
	}

	private void waitForField(FileTailingMessageProducerSupport adapter, String field) throws Exception {
		int n = 0;
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		while (n < 100) {
			if (accessor.getPropertyValue(field) == null) {
				Thread.sleep(100);
			}
			else {
				return;
			}
		}
		fail("adapter failed to start");
	}

	@Test
	@TailAvailable
	public void canHandleFilenameHavingSpecialCharacters() throws Exception {
		File file = File.createTempFile("foo bar", " -c 1");
		file.delete();

		OSDelegatingFileTailingMessageProducer adapter = new OSDelegatingFileTailingMessageProducer();
		adapter.setOptions(TAIL_OPTIONS_FOLLOW_NAME_ALL_LINES);
		adapter.setFile(file);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		adapter.setTaskScheduler(taskScheduler);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setTailAttemptsDelay(500);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();

		adapter.start();
		waitForField(adapter, "stdOutReader");

		FileOutputStream fos = new FileOutputStream(file);
		fos.write(("hello foobar\n").getBytes());
		fos.close();

		Message<?> message = outputChannel.receive(10000);
		assertThat(message).as("expected a non-null message").isNotNull();
		assertThat(message.getPayload()).isEqualTo("hello foobar");

		adapter.stop();
		file.delete();
		taskScheduler.destroy();
	}

}
