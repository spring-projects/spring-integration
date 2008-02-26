/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.mail;

import org.springframework.integration.message.AbstractMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;

/**
 * Message mapper for transforming integration messages with a String payload
 * into simple text e-mail messages. The body of the e-mail message will be the
 * content of the integration message's payload.
 * 
 * @author Marius Bogoevici
 */
public class TextMailMessageMapper extends AbstractMessageMapper<String, MailMessage> {

	public Message<String> toMessage(MailMessage source) {
		Assert.isInstanceOf(SimpleMailMessage.class, source, "source must be a SimpleMailMessage");
		return new StringMessage(((SimpleMailMessage) source).getText());
	}

	public MailMessage fromMessage(Message<String> stringMessage) {
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setText(stringMessage.getPayload());
		return mailMessage;
	}

}
