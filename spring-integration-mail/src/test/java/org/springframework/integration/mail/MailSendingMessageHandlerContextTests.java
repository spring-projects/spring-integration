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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.DataInputStream;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class MailSendingMessageHandlerContextTests {

	@Autowired
	@Qualifier("mailSendingMessageConsumer")
	private MailSendingMessageHandler handler;

	@Autowired
	private StubJavaMailSender mailSender;

	@Autowired
	private StubMailSender simpleMailSender;

	@Autowired
	private MessageChannel sendMailOutboundChainChannel;

	@Autowired
	private MessageChannel simpleEmailChannel;

	@Autowired
	private BeanFactory beanFactory;


	@BeforeEach
	public void reset() {
		this.mailSender.reset();
	}

	@Test
	public void stringMessagesWithConfiguration() {
		this.handler.handleMessage(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertThat(this.mailSender.getSentMimeMessages().size()).as("no mime message should have been sent")
				.isEqualTo(0);
		assertThat(this.mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		assertThat(this.mailSender.getSentSimpleMailMessages().get(0)).as("message content different from expected")
				.isEqualTo(mailMessage);
	}

	@Test
	public void byteArrayMessage() throws Exception {
		byte[] payload = { 1, 2, 3 };
		org.springframework.messaging.Message<?> message =
				MessageBuilder.withPayload(payload)
						.setHeader(MailHeaders.ATTACHMENT_FILENAME, "attachment.txt")
						.setHeader(MailHeaders.TO, MailTestsHelper.TO)
						.build();
		this.handler.handleMessage(message);
		assertThat(this.mailSender.getSentMimeMessages().size()).as("no mime message should have been sent")
				.isEqualTo(1);
		assertThat(this.mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(0);
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
	public void byteArrayMessageWithoutAttachmentFileName() {
		byte[] payload = { 1, 2, 3 };
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> this.handler.handleMessage(new GenericMessage<>(payload)));
	}

	@Test //INT-2275
	public void mailOutboundChannelAdapterWithinChain() {
		assertThat(this.beanFactory
				.getBean("org.springframework.integration.handler.MessageHandlerChain#0$child" +
						".mail-outbound-channel-adapter-within-chain.handler"))
				.isNotNull();
		this.sendMailOutboundChainChannel.send(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertThat(this.mailSender.getSentMimeMessages().size()).as("no mime message should have been sent")
				.isEqualTo(0);
		assertThat(this.mailSender.getSentSimpleMailMessages().size()).as("only one simple message must be sent")
				.isEqualTo(1);
		assertThat(this.mailSender.getSentSimpleMailMessages().get(0)).as("message content different from expected")
				.isEqualTo(mailMessage);
	}

	@Test
	public void testOutboundChannelAdapterWithSimpleMailSender() {
		this.simpleEmailChannel.send(MailTestsHelper.createIntegrationMessage());
		assertThat(this.simpleMailSender.getSentMessages().size()).isEqualTo(1);
		assertThat(this.simpleMailSender.getSentMessages().get(0)).isEqualTo(MailTestsHelper.createSimpleMailMessage());

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.simpleEmailChannel.send(new GenericMessage<>(new byte[0])))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() ->
						this.simpleEmailChannel.send(MessageBuilder.withPayload("foo")
								.setHeader(MailHeaders.CONTENT_TYPE, "text/plain")
								.setHeader(MailHeaders.TO, "foo@com.foo")
								.build()))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() ->
						this.simpleEmailChannel.send(new GenericMessage<>(this.mailSender.createMimeMessage())))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");
	}

}
