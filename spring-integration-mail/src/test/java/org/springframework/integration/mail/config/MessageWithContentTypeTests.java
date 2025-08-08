/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.config;

import java.io.FileReader;
import java.io.StringWriter;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class MessageWithContentTypeTests {

	@Test
	@Disabled
	public void testSendEmail() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext(
				"MessageWithContentTypeTests-context.xml", this.getClass());
		MessageChannel inputChannel = ac.getBean("inputChannel", MessageChannel.class);
		StringWriter writer = new StringWriter();
		FileReader reader = new FileReader("src/test/java/org/springframework/integration/mail/config/test.html");
		FileCopyUtils.copy(reader, writer);
		inputChannel.send(new GenericMessage<>(writer.getBuffer().toString()));
		ac.close();
	}

	@Test
	public void testMessageConversionWithHtmlAndContentType() throws Exception {
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
		doAnswer(invocation -> {
			MimeMessage mimeMessage = invocation.getArgument(0);
			assertThat(mimeMessage.getDataHandler().getContentType()).isEqualTo("text/html");
			return null;
		}).when(sender).send(Mockito.any(MimeMessage.class));

		// handle message
		handler.handleMessage(message);

		verify(sender, times(1)).send(Mockito.any(MimeMessage.class));
	}

	private static class TestMimeMessage extends MimeMessage {

		TestMimeMessage() {
			super(Session.getDefaultInstance(new Properties()));
		}

	}

}
