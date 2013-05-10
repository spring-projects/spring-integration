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
package org.springframework.integration.file.tail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport.FileTailingEvent;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class FileTailingMessageProducerTests {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	private File testDir;

	@Before
	public void setup() {
		File f = new File(tmpDir, "FileTailingMessageProducerTests");
		f.mkdir();
		this.testDir = f;
	}

	@Test
	public void testOS() throws Exception {
		OSDelegatingFileTailingMessageProducer adapter = new OSDelegatingFileTailingMessageProducer();
		testGuts(adapter, "reader");
	}

	@Test
	public void testApache() throws Exception {
		ApacheCommonsFileTailingMessageProducer adapter = new ApacheCommonsFileTailingMessageProducer();
		adapter.setMissingFileDelay(500);
		adapter.setPollingDelay(100);
		testGuts(adapter, "tailer");
	}

	private void testGuts(FileTailingMessageProducerSupport adapter, String field)
			throws Exception {
		final List<FileTailingEvent> events = new ArrayList<FileTailingEvent>();
		adapter.setApplicationEventPublisher(new ApplicationEventPublisher() {
			@Override
			public void publishEvent(ApplicationEvent event) {
				FileTailingEvent tailEvent = (FileTailingEvent) event;
				logger.warn(event);
				events.add(tailEvent);
			}
		});
		adapter.setFile(new File(testDir, "foo"));
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
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
		for (int i = 0; i < 50; i++) {
			Message<?> message = outputChannel.receive(5000);
			assertNotNull(message);
			assertEquals("hello" + i, message.getPayload());
		}
		file.renameTo(renamed);
		foo.close();
		foo = new FileOutputStream(file);
		if (adapter instanceof ApacheCommonsFileTailingMessageProducer) {
			Thread.sleep(1000);
		}
		for (int i = 50; i < 100; i++) {
			foo.write(("hello" + i + "\n").getBytes());
		}
		foo.flush();
		for (int i = 50; i < 100; i++) {
			Message<?> message = outputChannel.receive(3000);
			assertNotNull(message);
			assertEquals("hello" + i, message.getPayload());
		}
		foo.close();
		adapter.stop();
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

}
