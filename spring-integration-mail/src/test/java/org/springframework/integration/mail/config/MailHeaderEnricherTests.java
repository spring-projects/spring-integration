/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MailHeaderEnricherTests {

	@Autowired
	@Qualifier("literalValuesInput")
	private MessageChannel literalValuesInput;

	@Autowired
	@Qualifier("expressionsInput")
	private MessageChannel expressionsInput;

	@Test
	public void literalValues() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.literalValuesInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<>("test"));
		Map<String, Object> headers = result.getHeaders();
		assertThat(headers.get(MailHeaders.TO)).isEqualTo("test.to");
		assertThat(headers.get(MailHeaders.CC)).isEqualTo("test.cc");
		assertThat(headers.get(MailHeaders.BCC)).isEqualTo("test.bcc");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("test.from");
		assertThat(headers.get(MailHeaders.REPLY_TO)).isEqualTo("test.reply-to");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("test.subject");
		assertThat(headers.get(MailHeaders.ATTACHMENT_FILENAME)).isEqualTo("foo.txt");
		assertThat(headers.get(MailHeaders.MULTIPART_MODE)).isEqualTo("1");
	}

	@Test
	public void expressions() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.expressionsInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<>("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertThat(headers.get(MailHeaders.TO)).isEqualTo("foo.to");
		assertThat(headers.get(MailHeaders.CC)).isEqualTo("foo.cc");
		assertThat(headers.get(MailHeaders.BCC)).isEqualTo("foo.bcc");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("foo.from");
		assertThat(headers.get(MailHeaders.REPLY_TO)).isEqualTo("foo.reply-to");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("foo.subject");
	}

}
