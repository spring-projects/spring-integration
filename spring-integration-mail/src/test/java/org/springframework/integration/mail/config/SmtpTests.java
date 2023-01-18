/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.mail.config;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 *
 * @since 5.0
 *
 */
public class SmtpTests {

	private static GreenMail smtpServer;

	@BeforeAll
	public static void setup() {
		ServerSetup smtp = ServerSetupTest.SMTP.dynamicPort();
		smtp.setServerStartupTimeout(10000);
		smtpServer = new GreenMail(smtp);
		smtpServer.setUser("user", "pw");
		smtpServer.start();
	}

	@AfterAll
	public static void tearDown() {
		smtpServer.stop();
	}

	@Test
	public void testSmtp() throws Exception {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("localhost");
		mailSender.setPort(smtpServer.getSmtp().getPort());
		mailSender.setUsername("user");
		mailSender.setPassword("pw");
		MailSendingMessageHandler handler = new MailSendingMessageHandler(mailSender);

		handler.handleMessage(MessageBuilder.withPayload("foo")
				.setHeader(MailHeaders.TO, new String[] {"bar@baz"})
				.setHeader(MailHeaders.FROM, "foo@bar")
				.setHeader(MailHeaders.SUBJECT, "foo")
				.build());

		assertThat(smtpServer.waitForIncomingEmail(10000, 1)).isTrue();

		assertThat(smtpServer.getReceivedMessages().length > 0).isTrue();
		MimeMessage message = smtpServer.getReceivedMessages()[0];
		assertThat(message.getFrom()).containsOnly(new InternetAddress("foo@bar"));
		assertThat(message.getRecipients(RecipientType.TO)).containsOnly(new InternetAddress("bar@baz"));
		assertThat(message.getSubject()).isEqualTo("foo");
		assertThat(message.getContent()).asString().isEqualTo("foo");
	}

}
