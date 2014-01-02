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

package org.springframework.integration.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MailReceivingMessageSourceTests {

	@Test
	public void testPolling() {
		StubMailReceiver mailReceiver = new StubMailReceiver();
		MimeMessage message1 = Mockito.mock(MimeMessage.class);
		MimeMessage message2 = Mockito.mock(MimeMessage.class);
		MimeMessage message3 = Mockito.mock(MimeMessage.class);
		MimeMessage message4 = Mockito.mock(MimeMessage.class);

		mailReceiver.messages.add(new javax.mail.Message[] { message1 });
		mailReceiver.messages.add(new javax.mail.Message[] { message2, message3 });
		mailReceiver.messages.add(new javax.mail.Message[] { message4 });

		MailReceivingMessageSource source = new MailReceivingMessageSource(mailReceiver);
		assertEquals("Wrong message for number 1", message1, source.receive().getPayload());
		assertEquals("Wrong message for number 2", message2, source.receive().getPayload());
		assertEquals("Wrong message for number 3", message3, source.receive().getPayload());
		assertEquals("Wrong message for number 4", message4, source.receive().getPayload());
		assertNull("Expected null after exhausting all messages", source.receive());
	}


	@SuppressWarnings("unused")
	private static class StubMailReceiver implements MailReceiver {

		private final ConcurrentLinkedQueue<javax.mail.Message[]> messages = new ConcurrentLinkedQueue<javax.mail.Message[]>();

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

		public MailReceiverContext getTransactionContext() {
			return null;
		}

		public void closeContextAfterSuccess(MailReceiverContext context) {
		}

		public void closeContextAfterFailure(MailReceiverContext context) {
		}

	}

}
