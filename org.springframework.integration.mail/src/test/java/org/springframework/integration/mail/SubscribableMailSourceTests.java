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

package org.springframework.integration.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;

/**
 * @author Jonas Partner
 */
public class SubscribableMailSourceTests {

	@Test
	public void testReceive() throws Exception {
		javax.mail.Message message = EasyMock.createMock(MimeMessage.class);
		StubFolderConnection folderConnection = new StubFolderConnection(message);
		QueueChannel channel = new QueueChannel();
		ListeningMailSource mailSource = new ListeningMailSource(folderConnection);
		mailSource.setOutputChannel(channel);
		mailSource.start();
		Message<?> result = channel.receive(1000);
		mailSource.stop();
		assertNotNull(result);
		assertEquals("Wrong payload", message, result.getPayload());
		mailSource.stop();
	}


	private static class StubFolderConnection implements FolderConnection {

		private final ConcurrentLinkedQueue<javax.mail.Message> messages = new ConcurrentLinkedQueue<javax.mail.Message>();

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

}
