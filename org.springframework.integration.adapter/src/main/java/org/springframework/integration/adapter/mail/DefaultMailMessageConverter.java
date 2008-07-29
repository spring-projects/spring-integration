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

import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class DefaultMailMessageConverter implements MailMessageConverter {

	private DefaultMailMessageHeaderMapper headerMapper = new DefaultMailMessageHeaderMapper();

	@SuppressWarnings("unchecked")
	public Message create(MimeMessage mailMessage) {
		try {
			Map<String, Object> header = headerMapper.mapToMessageHeaders(mailMessage);
			GenericMessage<Object> message = new GenericMessage<Object>(mailMessage.getContent(), header);
			return message;
		}
		catch (Exception e) {
			throw new MessagingException("Conversion of MailMessage failed", e);
		}
	}

}
