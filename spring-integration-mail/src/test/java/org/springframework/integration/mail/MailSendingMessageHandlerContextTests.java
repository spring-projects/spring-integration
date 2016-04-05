/*
 * Copyright 2002-2015 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@RunWith(value = SpringJUnit4ClassRunner.class)
@ContextConfiguration
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


	@Before
	public void reset() {
		this.mailSender.reset();
	}

	@Test
	public void stringMessagesWithConfiguration() {
		this.handler.handleMessage(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertEquals("no mime message should have been sent",
				0, this.mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				1, this.mailSender.getSentSimpleMailMessages().size());
		assertEquals("message content different from expected",
				mailMessage, this.mailSender.getSentSimpleMailMessages().get(0));
	}

	@Test
	public void byteArrayMessage() throws Exception {
		byte[] payload = {1, 2, 3};
		org.springframework.messaging.Message<?> message =
				MessageBuilder.withPayload(payload)
				.setHeader(MailHeaders.ATTACHMENT_FILENAME, "attachment.txt")
				.setHeader(MailHeaders.TO, MailTestsHelper.TO)
				.build();
		this.handler.handleMessage(message);
		assertEquals("no mime message should have been sent",
				1, this.mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				0, this.mailSender.getSentSimpleMailMessages().size());
		byte[] buffer = new byte[1024];
		MimeMessage mimeMessage = this.mailSender.getSentMimeMessages().get(0);
		assertTrue("message must be multipart", mimeMessage.getContent() instanceof Multipart);
		int size = new DataInputStream(((Multipart) mimeMessage.getContent()).getBodyPart(0).getInputStream()).read(buffer);
		assertEquals("buffer size does not match", payload.length, size);
		byte[] messageContent = new byte[size];
		System.arraycopy(buffer, 0, messageContent, 0, payload.length);
		assertArrayEquals("buffer content does not match", payload, messageContent);
		assertEquals(mimeMessage.getRecipients(Message.RecipientType.TO).length, MailTestsHelper.TO.length);
	}

	@Test(expected = MessageMappingException.class)
	public void byteArrayMessageWithoutAttachmentFileName() throws Exception {
		byte[] payload = {1, 2, 3};
		this.handler.handleMessage(new GenericMessage<byte[]>(payload));
	}

	@Test //INT-2275
	public void mailOutboundChannelAdapterWithinChain() {
		assertNotNull(this.beanFactory.getBean("org.springframework.integration.handler.MessageHandlerChain#0$child.mail-outbound-channel-adapter-within-chain.handler"));
		this.sendMailOutboundChainChannel.send(MailTestsHelper.createIntegrationMessage());
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertEquals("no mime message should have been sent", 0, this.mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent", 1, this.mailSender.getSentSimpleMailMessages().size());
		assertEquals("message content different from expected", mailMessage, this.mailSender.getSentSimpleMailMessages().get(0));
	}

	@Test
	public void testOutboundChannelAdapterWithSimpleMailSender() {
		this.simpleEmailChannel.send(MailTestsHelper.createIntegrationMessage());
		assertEquals(1, this.simpleMailSender.getSentMessages().size());
		assertEquals(MailTestsHelper.createSimpleMailMessage(), this.simpleMailSender.getSentMessages().get(0));

		try {
			this.simpleEmailChannel.send(new GenericMessage<byte[]>(new byte[0]));
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(),
					containsString("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'"));
		}

		try {
			this.simpleEmailChannel.send(MessageBuilder.withPayload("foo")
					.setHeader(MailHeaders.CONTENT_TYPE, "text/plain")
					.setHeader(MailHeaders.TO, "foo@com.foo")
					.build());
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(),
					containsString("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'"));
		}

		try {
			this.simpleEmailChannel.send(new GenericMessage<MimeMessage>(this.mailSender.createMimeMessage()));
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(),
					containsString("this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'"));
		}
	}

}
