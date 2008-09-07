/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

/**
 * @author Jonas Partner
 */
public class SubscribableMailSourceTests {

	TaskExecutor executor;

	@Before
	public void setUp() {
		executor = new ConcurrentTaskExecutor();
	}

	@Test
	public void testReceive() throws Exception {
		javax.mail.Message message = EasyMock.createMock(MimeMessage.class);
		StubFolderConnection folderConnection = new StubFolderConnection(message);
		QueueChannel channel = new QueueChannel();
		SubscribableMailSource mailSource = new SubscribableMailSource(folderConnection, executor);
		mailSource.setOutputChannel(channel);
		mailSource.setConverter(new StubMessageConvertor());
		mailSource.start();
		Message<?> result = channel.receive(1000);
		mailSource.stop();
		assertNotNull(result);
		assertEquals("Wrong payload", message, result.getPayload());
	}


	private static class StubFolderConnection implements FolderConnection {

		ConcurrentLinkedQueue<javax.mail.Message> messages = new ConcurrentLinkedQueue<javax.mail.Message>();

		public StubFolderConnection(javax.mail.Message message) {
			messages.add(message);
		}

		public javax.mail.Message[] receive() {
			javax.mail.Message msg = messages.poll();
			if (msg == null) {
				return new javax.mail.Message[] {};
			}
			return new javax.mail.Message[] { msg };
		}

		public boolean isRunning() {
			return false;
		}

		public void start() {
		}

		public void stop() {
		}
	}


	private static class StubMessageConvertor implements MailMessageConverter {

		@SuppressWarnings("unchecked")
		public Message create(MimeMessage mailMessage) {
			return new GenericMessage<MimeMessage>(mailMessage);
		}
	}

}
