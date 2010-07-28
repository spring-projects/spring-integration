/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MailHeaderEnricherTests {

	@Autowired @Qualifier("literalValuesInput")
	private MessageChannel literalValuesInput;

	@Autowired @Qualifier("expressionsInput")
	private MessageChannel expressionsInput;

	@Test
	public void literalValues() {
		MessagingTemplate template = new MessagingTemplate(literalValuesInput);
		Message<?> result = template.sendAndReceive(new StringMessage("test"));
		Map<String, Object> headers = result.getHeaders();
		assertEquals("test.to", headers.get(MailHeaders.TO));
		assertEquals("test.cc", headers.get(MailHeaders.CC));
		assertEquals("test.bcc", headers.get(MailHeaders.BCC));
		assertEquals("test.from", headers.get(MailHeaders.FROM));
		assertEquals("test.reply-to", headers.get(MailHeaders.REPLY_TO));
		assertEquals("test.subject", headers.get(MailHeaders.SUBJECT));
	}

	@Test
	public void expressions() {
		MessagingTemplate template = new MessagingTemplate(expressionsInput);
		Message<?> result = template.sendAndReceive(new StringMessage("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertEquals("foo.to", headers.get(MailHeaders.TO));
		assertEquals("foo.cc", headers.get(MailHeaders.CC));
		assertEquals("foo.bcc", headers.get(MailHeaders.BCC));
		assertEquals("foo.from", headers.get(MailHeaders.FROM));
		assertEquals("foo.reply-to", headers.get(MailHeaders.REPLY_TO));
		assertEquals("foo.subject", headers.get(MailHeaders.SUBJECT));
	}

}
