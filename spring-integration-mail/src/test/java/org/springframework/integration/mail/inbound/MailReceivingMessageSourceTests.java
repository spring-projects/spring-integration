/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.inbound;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
		MimeMessage message1 = mock();
		MimeMessage message2 = mock();
		MimeMessage message3 = mock();
		MimeMessage message4 = mock();

		mailReceiver.messages.add(new jakarta.mail.Message[] {message1});
		mailReceiver.messages.add(new jakarta.mail.Message[] {message2, message3});
		mailReceiver.messages.add(new jakarta.mail.Message[] {message4});

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
		public jakarta.mail.Message[] receive() {
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
