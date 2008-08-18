/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;

/**
 * Message mapper for transforming integration messages into simple text
 * e-mail messages. The body of the e-mail message will be the result of
 * invoking the message payload's toString() method.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class TextMailMessageMapper implements MessageMapper<String, MailMessage> {

	public MailMessage mapMessage(Message<String> message) {
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setText(message.getPayload().toString());
		return mailMessage;
	}

}
