/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@RunWith(value = SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/springframework/integration/mail/mailSendingMessageHandlerContextTests.xml"})
public class MailSendingMessageHandlerContextTests {

	@Autowired
	@Qualifier("mailSendingMessageConsumer")
	private MailSendingMessageHandler handler;

	@Autowired
	private StubJavaMailSender mailSender;

	@Autowired
	private MessageChannel sendMailOutboundChainChannel;

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

}
