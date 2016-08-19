/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.integration.test.mail.TestMailServer.SmtpServer;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class SmtpTests {

	private static final SmtpServer smtpServer = TestMailServer.smtp(0);

	@BeforeClass
	public static void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!smtpServer.isListening())) {
			Thread.sleep(100);
		}
		assertTrue(n < 100);
	}

	@AfterClass
	public static void tearDown() {
		smtpServer.stop();
	}

	@Test
	public void testSmtp() throws Exception {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("localhost");
		mailSender.setPort(smtpServer.getPort());
		mailSender.setUsername("user");
		mailSender.setPassword("pw");
		MailSendingMessageHandler handler = new MailSendingMessageHandler(mailSender);

		handler.handleMessage(MessageBuilder.withPayload("foo")
				.setHeader(MailHeaders.TO, new String[] {"bar@baz"})
				.setHeader(MailHeaders.FROM, "foo@bar")
				.setHeader(MailHeaders.SUBJECT, "foo")
				.build());

		int n = 0;
		while (n++ < 100 && smtpServer.getMessages().size() == 0) {
			Thread.sleep(100);
		}

		assertTrue(smtpServer.getMessages().size() > 0);
		String message = smtpServer.getMessages().get(0);
		assertThat(message, endsWith("foo\n"));
		assertThat(message, containsString("foo@bar"));
		assertThat(message, containsString("bar@baz"));
		assertThat(message, containsString("user:user"));
		assertThat(message, containsString("password:pw"));

	}

}
