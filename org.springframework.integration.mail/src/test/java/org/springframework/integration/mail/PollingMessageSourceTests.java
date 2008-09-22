/*
 * Copyright 2002-2007 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import org.springframework.integration.mail.FolderConnection;
import org.springframework.integration.mail.MailMessageConverter;
import org.springframework.integration.mail.PollingMailSource;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

public class PollingMessageSourceTests {

	@Test
	public void testPolling(){
		StubFolderConnection folderConnection = new StubFolderConnection();
	
		MimeMessage messageOne = EasyMock.createMock(MimeMessage.class);
		MimeMessage messageTwo = EasyMock.createMock(MimeMessage.class);
		MimeMessage messageThree = EasyMock.createMock(MimeMessage.class);
		MimeMessage messageFour = EasyMock.createMock(MimeMessage.class);
		
		folderConnection.messages.add(new javax.mail.Message[]{messageOne});
		folderConnection.messages.add(new javax.mail.Message[]{messageTwo,messageThree});
		folderConnection.messages.add(new javax.mail.Message[]{messageFour});
		
		PollingMailSource pollingMailSource = new PollingMailSource(folderConnection);
		pollingMailSource.setConverter(new StubMessageConvertor());
		
		
		assertEquals("Wrong message for number 1", messageOne, pollingMailSource.receive().getPayload());
		assertEquals("Wrong message for number 2", messageTwo, pollingMailSource.receive().getPayload());
		assertEquals("Wrong message for number 3", messageThree, pollingMailSource.receive().getPayload());
		assertEquals("Wrong message for number 4", messageFour, pollingMailSource.receive().getPayload());
		assertNull("Expected null after exhausting all messages",pollingMailSource.receive());
	}
	
	
	
	private static class StubFolderConnection implements FolderConnection {

		ConcurrentLinkedQueue<javax.mail.Message[]> messages = new ConcurrentLinkedQueue<javax.mail.Message[]>();

		
		
		public javax.mail.Message[] receive() {
			return messages.poll();
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
