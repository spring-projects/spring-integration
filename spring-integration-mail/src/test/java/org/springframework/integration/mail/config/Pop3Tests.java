/*
 * Copyright 2016-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.integration.test.mail.TestMailServer.Pop3Server;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class Pop3Tests {

	private static final Pop3Server pop3Server = TestMailServer.pop3(0);

	@BeforeAll
	public static void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!pop3Server.isListening())) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();
	}

	@AfterAll
	public static void tearDown() {
		pop3Server.stop();
	}

	@Test
	public void testPop3() {
		Pop3MailReceiver receiver = new Pop3MailReceiver("localhost", pop3Server.getPort(), "user", "pw");
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
		assertThat(message.getPayload()).isEqualTo("foo\r\n\r\n");
	}

}
