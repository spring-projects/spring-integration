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

package org.springframework.integration.mail.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.mail.Folder;
import javax.mail.Message;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ImapIdleIntegrationTests {

	@SuppressWarnings("resource")
	@Test
	public void testWithTransactionSynchronization() throws Exception {
		final AtomicBoolean block = new AtomicBoolean(false);
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("imap-idle-mock-integration-config.xml", this.getClass());
		PostTransactionProcessor processor = context.getBean("syncProcessor", PostTransactionProcessor.class);
		ImapIdleChannelAdapter adapter = context.getBean("customAdapter", ImapIdleChannelAdapter.class);
		assertThat(TestUtils.getPropertyValue(adapter, "applicationEventPublisher")).isNotNull();
		ImapMailReceiver receiver = TestUtils.getPropertyValue(adapter, "mailReceiver", ImapMailReceiver.class);

		// setup mock scenario
		receiver = spy(receiver);

		doAnswer(invocation -> {
			// ensures that waitForNewMessages call blocks after a first execution
			// to emulate the behavior of IDLE
			if (block.get()) {
				Thread.sleep(5000);
			}
			block.set(true);
			return null;
		}).when(receiver).waitForNewMessages();

		Message m1 = mock(Message.class);
		doReturn(new Message[]{ m1 }).when(receiver).receive();

		Folder folder = mock(Folder.class);
		when(folder.isOpen()).thenReturn(true);
		Field folderField = ReflectionUtils.findField(ImapMailReceiver.class, "folder");
		folderField.setAccessible(true);
		folderField.set(receiver, folder);

		Field mrField = ImapIdleChannelAdapter.class.getDeclaredField("mailReceiver");
		mrField.setAccessible(true);
		mrField.set(adapter, receiver);
		// end mock setup

		final CountDownLatch txProcessorLatch = new CountDownLatch(1);
		doAnswer(invocation -> {
			txProcessorLatch.countDown();
			return null;
		}).when(processor).process(any(Message.class));

		adapter.start();

		assertThat(txProcessorLatch.await(10, TimeUnit.SECONDS)).isTrue();

		adapter.stop();
		context.close();

	}

	public interface PostTransactionProcessor {

		void process(Message mailMessage);

	}

}
