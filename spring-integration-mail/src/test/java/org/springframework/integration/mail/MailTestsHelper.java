/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mail;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.messaging.Message;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 */
public class MailTestsHelper {

	public static final String SUBJECT = "Some subject";

	public static final String MESSAGE_TEXT = "Some text";

	public static final String[] TO = new String[] {
			"toRecipient1@springframework.org", "toRecipient2@springframework.org"};

	public static final String[] CC = new String[] {
			"ccRecipient1@springframework.org", "ccRecipient2@springframework.org"};

	public static final String[] BCC = new String[] {
			"bccRecipient1@springframework.org", "bccRecipient2@springframework.org"};

	public static final String FROM = "from@springframework.org";

	public static final String REPLY_TO = "replyTo@springframework.org";

	private MailTestsHelper() {
		super();
	}

	public static SimpleMailMessage createSimpleMailMessage() {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setBcc(BCC);
		message.setCc(CC);
		message.setTo(TO);
		message.setSubject(SUBJECT);
		message.setReplyTo(REPLY_TO);
		message.setFrom(FROM);
		message.setText(MESSAGE_TEXT);
		return message;
	}

	public static Message<String> createIntegrationMessage() {
		return MessageBuilder.withPayload(MailTestsHelper.MESSAGE_TEXT)
				.setHeader(MailHeaders.SUBJECT, MailTestsHelper.SUBJECT)
				.setHeader(MailHeaders.TO, MailTestsHelper.TO)
				.setHeader(MailHeaders.CC, MailTestsHelper.CC)
				.setHeader(MailHeaders.BCC, MailTestsHelper.BCC)
				.setHeader(MailHeaders.FROM, MailTestsHelper.FROM)
				.setHeader(MailHeaders.REPLY_TO, MailTestsHelper.REPLY_TO)
				.build();
	}

}
