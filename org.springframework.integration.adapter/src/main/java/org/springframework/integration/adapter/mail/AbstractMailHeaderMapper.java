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

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.adapter.MessageHeaderMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessagingException;
import org.springframework.mail.javamail.MimeMailMessage;

/**
 * 
 * @author Jonas Partner
 *
 */
public abstract class AbstractMailHeaderMapper implements MessageHeaderMapper<MimeMessage> {

	/**
	 * Retrieve the subject of an e-mail message from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the e-mail message subject
	 */
	protected abstract String getSubject(MessageHeaders message);

	/**
	 * Retrieve the recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return recipients list (TO)
	 */
	protected abstract String[] getTo(MessageHeaders message);

	/**
	 * Retrieve the CC recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return CC recipients list (e-mail addresses)
	 */
	protected abstract String[] getCc(MessageHeaders message);

	/**
	 * Retrieve the BCC recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return BCC recipients list (e-mail addresses)
	 */
	protected abstract String[] getBcc(MessageHeaders message);

	/**
	 * Retrieve the From: e-mail address from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the From: e-mail address
	 */
	protected abstract String getFrom(MessageHeaders message);

	/**
	 * Retrieve the Reply To: e-mail address from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the ReplyTo: e-mail address
	 */
	protected abstract String getReplyTo(MessageHeaders message);

	private final Log logger = LogFactory.getLog(this.getClass());

	public void mapFromMessageHeaders(MessageHeaders headers, MimeMessage mailMessage) {
		MimeMailMessage message = new MimeMailMessage(mailMessage);

		final String subject = getSubject(headers);
		final String[] to = getTo(headers);
		final String[] cc = getCc(headers);
		final String[] bcc = getBcc(headers);
		final String from = getFrom(headers);
		final String replyTo = getReplyTo(headers);
		if (subject != null) {
			message.setSubject(subject);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'SUBJECT' property available for mail message");
		}
		if (to != null) {
			message.setTo(to);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'TO' property available for mail message");
		}
		if (cc != null) {
			message.setCc(cc);
		}
		if (bcc != null) {
			message.setBcc(bcc);
		}
		if (from != null) {
			message.setFrom(from);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'FROM' property available for mail message");
		}
		if (replyTo != null) {
			message.setReplyTo(replyTo);
		}
	}

	public Map<String,Object> mapToMessageHeaders(MimeMessage mailMessage) {
		try {
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put(MailAttributeKeys.FROM, convertToString(mailMessage.getFrom()));
			headers.put(MailAttributeKeys.BCC, convertToStringArray(mailMessage.getRecipients(RecipientType.BCC)));
			headers.put(MailAttributeKeys.CC, convertToStringArray(mailMessage.getRecipients(RecipientType.CC)));
			headers.put(MailAttributeKeys.TO, convertToStringArray(mailMessage.getRecipients(RecipientType.TO)));
			headers.put(MailAttributeKeys.REPLY_TO, convertToString(mailMessage.getReplyTo()));
			headers.put(MailAttributeKeys.SUBJECT, mailMessage.getSubject());
			return headers;
		}
		catch (Exception e) {
			throw new MessagingException("Conversion of MailMessage headers failed", e);
		}
	}

	protected String retrieveAsString(MessageHeaders headers, String key) {
		Object value = headers.get(key);
		return (value instanceof String) ? (String) value : null;
	}

	protected String[] retrieveAsStringArray(MessageHeaders headers, String key) {
		Object value = headers.get(key);
		if (value instanceof String[]) {
			return (String[]) value;
		}
		if (value instanceof String) {
			return new String[] { (String) value };
		}
		return null;
	}

	private String convertToString(Address[] addresses) {
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		if (addresses.length != 1) {
			throw new IllegalStateException("expected a single value but received an Array");
		}
		return addresses[0].toString();
	}

	private String[] convertToStringArray(Address[] addresses) {
		if (addresses != null) {
			String[] addressStrings = new String[addresses.length];
			for (int i = 0; i < addresses.length; i++) {
				addressStrings[i] = addresses[i].toString();
			}
			return addressStrings;
		}
		return new String[0];
	}

}
