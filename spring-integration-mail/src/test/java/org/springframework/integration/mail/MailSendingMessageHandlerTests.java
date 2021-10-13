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

import java.io.DataInputStream;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Marius Bogoevici
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class MailSendingMessageHandlerTests {

	private MailSendingMessageHandler handler;

	private StubJavaMailSender mailSender;


	@BeforeEach
	public void setUp() {
		this.mailSender = new StubJavaMailSender(new MimeMessage((Session) null));
		this.handler = new MailSendingMessageHandler(this.mailSender);
	}

	@AfterEach
	public void reset() {
		this.mailSender.reset();
	}


	@Test
	public void textMessage() {
		this.handler.handleMessage(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertThat(mailSender.getSentMimeMessages().size()).as("no mime message should have been sent").isEqualTo(0);
		assertThat(mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		assertThat(mailSender.getSentSimpleMailMessages().get(0)).as("message content different from expected")
				.isEqualTo(mailMessage);
	}

	@Test
	public void byteArrayMessage() throws Exception {
		byte[] payload = { 1, 2, 3 };
		org.springframework.messaging.Message<byte[]> message =
				MessageBuilder.withPayload(payload)
						.setHeader(MailHeaders.ATTACHMENT_FILENAME, "attachment.txt")
						.setHeader(MailHeaders.TO, MailTestsHelper.TO)
						.build();
		this.handler.handleMessage(message);
		byte[] buffer = new byte[1024];
		MimeMessage mimeMessage = this.mailSender.getSentMimeMessages().get(0);
		assertThat(mimeMessage.getContent() instanceof Multipart).as("message must be multipart").isTrue();
		int size = new DataInputStream(((Multipart) mimeMessage.getContent()).getBodyPart(0).getInputStream())
				.read(buffer);
		assertThat(size).as("buffer size does not match").isEqualTo(payload.length);
		byte[] messageContent = new byte[size];
		System.arraycopy(buffer, 0, messageContent, 0, payload.length);
		assertThat(messageContent).as("buffer content does not match").isEqualTo(payload);
		assertThat(MailTestsHelper.TO.length).isEqualTo(mimeMessage.getRecipients(Message.RecipientType.TO).length);
	}

	@Test
	public void mailHeaders() {
		this.handler.handleMessage(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertThat(mailSender.getSentMimeMessages().size()).as("no mime message should have been sent").isEqualTo(0);
		assertThat(mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		assertThat(mailSender.getSentSimpleMailMessages().get(0)).as("message content different from expected")
				.isEqualTo(mailMessage);
	}

	@Test
	public void simpleMailMessage() {
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		String[] toHeaders = mailMessage.getTo();
		this.handler.handleMessage(MessageBuilder.withPayload(mailMessage).build());
		assertThat(mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		SimpleMailMessage sentMessage = mailSender.getSentSimpleMailMessages().get(0);
		assertThat(sentMessage.getTo()).isEqualTo(toHeaders);
	}

	@Test
	public void simpleMailMessageOverrideWithHeaders() {
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		mailMessage.getTo();
		this.handler.handleMessage(MessageBuilder.withPayload(mailMessage)
				.setHeader(MailHeaders.TO, new String[]{ "foo@bar.bam" }).build());
		assertThat(mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		SimpleMailMessage sentMessage = mailSender.getSentSimpleMailMessages().get(0);
		assertThat(sentMessage.getTo()[0]).isEqualTo("foo@bar.bam");
	}

}
