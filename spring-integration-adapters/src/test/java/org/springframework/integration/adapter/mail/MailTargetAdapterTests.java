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

package org.springframework.integration.adapter.mail;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Marius Bogoevici
 */
public class MailTargetAdapterTests {

	private MailTargetAdapter mailTargetAdapter;

	private StubJavaMailSender mailSender;

	private StaticMailHeaderGenerator staticMailHeaderGenerator;


	@Before
	public void setUp() throws Exception {
		this.mailSender = new StubJavaMailSender(new MimeMessage((Session) null));
		this.staticMailHeaderGenerator = new StaticMailHeaderGenerator();
		this.staticMailHeaderGenerator.setBcc(MailTestsHelper.BCC);
		this.staticMailHeaderGenerator.setCc(MailTestsHelper.CC);
		this.staticMailHeaderGenerator.setFrom(MailTestsHelper.FROM);
		this.staticMailHeaderGenerator.setReplyTo(MailTestsHelper.REPLY_TO);
		this.staticMailHeaderGenerator.setSubject(MailTestsHelper.SUBJECT);
		this.staticMailHeaderGenerator.setTo(MailTestsHelper.TO);
		this.mailTargetAdapter = new MailTargetAdapter(this.mailSender);
		this.mailTargetAdapter.afterPropertiesSet();
	}

	@Test
	public void testTextMessage() {
		this.mailTargetAdapter.setHeaderGenerator(this.staticMailHeaderGenerator);
		this.mailTargetAdapter.handle(new StringMessage(MailTestsHelper.MESSAGE_TEXT));
		SimpleMailMessage message = MailTestsHelper.createSimpleMailMessage();
		assertEquals("no mime message should have been sent",
				0, mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				1, mailSender.getSentSimpleMailMessages().size());
		assertEquals("message content different from expected",
				message, mailSender.getSentSimpleMailMessages().get(0));
	}

	@Test
	public void testByteArrayMessage() throws Exception {
		this.mailTargetAdapter.setHeaderGenerator(this.staticMailHeaderGenerator);
		byte[] payload = {1, 2, 3};
		this.mailTargetAdapter.handle(new GenericMessage<byte[]>(payload));
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

	@Test
	public void testDefaultMailHeaderGenerator() {
		StringMessage message = new StringMessage(MailTestsHelper.MESSAGE_TEXT);
		message.getHeader().setAttribute(MailAttributeKeys.SUBJECT, MailTestsHelper.SUBJECT);
		message.getHeader().setAttribute(MailAttributeKeys.TO, MailTestsHelper.TO);
		message.getHeader().setAttribute(MailAttributeKeys.CC, MailTestsHelper.CC);
		message.getHeader().setAttribute(MailAttributeKeys.BCC, MailTestsHelper.BCC);
		message.getHeader().setAttribute(MailAttributeKeys.FROM, MailTestsHelper.FROM);
		message.getHeader().setAttribute(MailAttributeKeys.REPLY_TO, MailTestsHelper.REPLY_TO);
		this.mailTargetAdapter.handle(message);
		SimpleMailMessage mailMessage = MailTestsHelper.createSimpleMailMessage();
		assertEquals("no mime message should have been sent",
				0, mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				1, mailSender.getSentSimpleMailMessages().size());
		assertEquals("message content different from expected",
				mailMessage, mailSender.getSentSimpleMailMessages().get(0));
	}

	@After
	public void reset() {
		this.mailSender.reset();
	}

}
