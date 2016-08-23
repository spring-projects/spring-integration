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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
 * @since 5.0
 *
 */
public class Pop3Tests {

	private static final Pop3Server pop3Server = TestMailServer.pop3(0);

	@BeforeClass
	public static void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!pop3Server.isListening())) {
			Thread.sleep(100);
		}
		assertTrue(n < 100);
	}

	@AfterClass
	public static void tearDown() {
		pop3Server.stop();
	}

	@Test
	public void testPop3() throws Exception {
		Pop3MailReceiver receiver = new Pop3MailReceiver("localhost", pop3Server.getPort(), "user", "pw");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		MailReceivingMessageSource source = new MailReceivingMessageSource(receiver);
		Message<?> message = source.receive();
		assertNotNull(message);
		MessageHeaders headers = message.getHeaders();
		assertEquals("Foo <foo@bar>", headers.get(MailHeaders.TO, String[].class)[0]);
		assertEquals("Bar <bar@baz>", headers.get(MailHeaders.FROM));
		assertEquals("Test Email", headers.get(MailHeaders.SUBJECT));
		assertEquals("foo\r\n\r\n", message.getPayload());
	}

}
