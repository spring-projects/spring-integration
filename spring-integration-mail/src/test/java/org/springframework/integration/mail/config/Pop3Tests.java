/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.Arrays;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 *
 * @since 5.0
 *
 */
public class Pop3Tests {

	private static GreenMail pop3Server;

	private static GreenMailUser user;

	@BeforeAll
	public static void setup() {
		ServerSetup pop3 = ServerSetupTest.POP3.dynamicPort();
		pop3.setServerStartupTimeout(10000);
		pop3Server = new GreenMail(pop3);
		user = pop3Server.setUser("user", "pw");
		pop3Server.start();
	}

	@AfterAll
	public static void tearDown() {
		pop3Server.stop();
	}

	@Test
	public void testPop3() throws MessagingException {
		MimeMessage mimeMessage =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>, Bar2 <bar2@baz>", "Test Email",
						"foo\r\n", pop3Server.getPop3().getServerSetup());
		mimeMessage.setRecipients(RecipientType.CC, "a@b, c@d");
		mimeMessage.setRecipients(RecipientType.BCC, "e@f, g@h");
		user.deliver(mimeMessage);

		Pop3MailReceiver receiver = new Pop3MailReceiver("localhost", pop3Server.getPop3().getPort(), "user", "pw");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		MailReceivingMessageSource source = new MailReceivingMessageSource(receiver);
		Message<?> message = source.receive();
		assertThat(message).isNotNull();
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(MailHeaders.TO, String[].class)[0]).isEqualTo("Foo <foo@bar>");
		assertThat(Arrays.toString(headers.get(MailHeaders.CC, String[].class))).isEqualTo("[a@b, c@d]");
		assertThat(Arrays.toString(headers.get(MailHeaders.BCC, String[].class))).isEqualTo("[e@f, g@h]");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>,Bar2 <bar2@baz>");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
		assertThat(message.getPayload()).isEqualTo("foo\r\n");
	}

}
