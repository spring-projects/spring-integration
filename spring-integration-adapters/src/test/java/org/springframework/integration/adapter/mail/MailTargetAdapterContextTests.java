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
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.adapter.mail.MailTargetAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Marius Bogoevici
 */
@RunWith(value = SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/org/springframework/integration/adapter/mail/mailTargetAdapter.xml"})
public class MailTargetAdapterContextTests {

	@Autowired
	private MailTargetAdapter mailTargetAdapter;

	@Autowired
	private StubJavaMailSender mailSender;


	@Before
	public void reset() {
		this.mailSender.reset();
	}

	@Test
	public void testStringMesssagesWithConfiguration() {
		this.mailTargetAdapter.send(new StringMessage(MailTestsHelper.MESSAGE_TEXT));
		SimpleMailMessage message = MailTestsHelper.createSimpleMailMessage();
		assertEquals("no mime message should have been sent",
				0, this.mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				1, this.mailSender.getSentSimpleMailMessages().size());
		assertEquals("message content different from expected",
				message, this.mailSender.getSentSimpleMailMessages().get(0));
	}

	@Test
	public void testByteArrayMessage() throws Exception {
		byte[] payload = {1, 2, 3};
		mailTargetAdapter.send(new GenericMessage<byte[]>(payload));
		assertEquals("no mime message should have been sent",
				1, mailSender.getSentMimeMessages().size());
		assertEquals("only one simple message must be sent",
				0, mailSender.getSentSimpleMailMessages().size());
		byte[] buffer = new byte[1024];
		MimeMessage mimeMessage = mailSender.getSentMimeMessages().get(0);
		assertTrue("message must be multipart", mimeMessage.getContent() instanceof Multipart);
		int size = new DataInputStream(((Multipart) mimeMessage.getContent()).getBodyPart(0).getInputStream()).read(buffer);
		assertEquals("buffer size does not match", payload.length, size);
		byte[] messageContent = new byte[size];
		System.arraycopy(buffer, 0, messageContent, 0, payload.length);
		assertArrayEquals("buffer content does not match", payload, messageContent);
		assertEquals(mimeMessage.getRecipients(Message.RecipientType.TO).length, MailTestsHelper.TO.length);
	}

}
