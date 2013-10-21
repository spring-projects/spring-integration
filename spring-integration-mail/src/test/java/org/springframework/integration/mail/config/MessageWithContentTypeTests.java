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
package org.springframework.integration.mail.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.FileCopyUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class MessageWithContentTypeTests {

	@Test
	@Ignore
	public void testSendEmail() throws Exception{
		ApplicationContext ac = new ClassPathXmlApplicationContext("MessageWithContentTypeTests-context.xml", this.getClass());
		MessageChannel inputChannel = ac.getBean("inputChannel", MessageChannel.class);
		StringWriter writer = new StringWriter();
		FileReader reader = new FileReader("src/test/java/org/springframework/integration/mail/config/test.html");
		FileCopyUtils.copy(reader, writer);
		inputChannel.send(new GenericMessage<String>(writer.getBuffer().toString()));
	}

	@Test
	public void testMessageConversionWithHtmlAndContentType() throws Exception{
		JavaMailSender sender = mock(JavaMailSender.class);
		MailSendingMessageHandler handler = new MailSendingMessageHandler(sender);
		StringWriter writer = new StringWriter();
		FileReader reader = new FileReader("src/test/java/org/springframework/integration/mail/config/test.html");
		FileCopyUtils.copy(reader, writer);
		Message<String> message = MessageBuilder.withPayload(writer.getBuffer().toString())
								.setHeader(MailHeaders.TO, "to")
								.setHeader(MailHeaders.FROM, "from")
								.setHeader(MailHeaders.CONTENT_TYPE, "text/html")
								.build();
		MimeMessage mMessage = new TestMimeMessage();
		// MOCKS
		when(sender.createMimeMessage()).thenReturn(mMessage);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				MimeMessage mimeMessage = (MimeMessage) invocation.getArguments()[0];
				assertEquals("text/html", mimeMessage.getDataHandler().getContentType());
				return null;
			}
		}).when(sender).send(Mockito.any(MimeMessage.class));

		// handle message
		handler.handleMessage(message);

		verify(sender, times(1)).send(Mockito.any(MimeMessage.class));
	}

	private static class TestMimeMessage extends MimeMessage{
		public TestMimeMessage() {
			super(Session.getDefaultInstance(new Properties()));
		}
	}
}
