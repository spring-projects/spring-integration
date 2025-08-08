/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

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
