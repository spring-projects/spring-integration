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

package org.springframework.integration.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
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

		mailReceiver.messages.add(new javax.mail.Message[]{ message1 });
		mailReceiver.messages.add(new javax.mail.Message[]{ message2, message3 });
		mailReceiver.messages.add(new javax.mail.Message[]{ message4 });

		MailReceivingMessageSource source = new MailReceivingMessageSource(mailReceiver);
		assertThat(source.receive().getPayload()).as("Wrong message for number 1").isEqualTo(message1);
		assertThat(source.receive().getPayload()).as("Wrong message for number 2").isEqualTo(message2);
		assertThat(source.receive().getPayload()).as("Wrong message for number 3").isEqualTo(message3);
		assertThat(source.receive().getPayload()).as("Wrong message for number 4").isEqualTo(message4);
		assertThat(source.receive()).as("Expected null after exhausting all messages").isNull();
	}


	@SuppressWarnings("unused")
	private static class StubMailReceiver implements MailReceiver {

		private final Queue<Message[]> messages = new ConcurrentLinkedQueue<>();

		StubMailReceiver() {
			super();
		}

		@Override
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

}
