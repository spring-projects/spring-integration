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

package org.springframework.integration.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.mail.MailMessage;

/**
 * Base implementation for {@link MailHeaderGenerator MailHeaderGenerators}.
 * This class is abstract. Subclasses must implement the corresponding template
 * methods to retrieve the values for the mail message's subject, recipients,
 * and from/reply-to addresses based on the integration {@link Message}.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public abstract class AbstractMailHeaderGenerator implements MailHeaderGenerator {

	private final Log logger = LogFactory.getLog(this.getClass());


	/**
	 * Retrieve the subject of an e-mail message from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the e-mail message subject
	 */
	protected abstract String getSubject(Message<?> message);

	/**
	 * Retrieve the recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return recipients list (TO)
	 */
	protected abstract String[] getTo(Message<?> message);

	/**
	 * Retrieve the CC recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return CC recipients list (e-mail addresses)
	 */
	protected abstract String[] getCc(Message<?> message);

	/**
	 * Retrieve the BCC recipients list from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return BCC recipients list (e-mail addresses)
	 */
	protected abstract String[] getBcc(Message<?> message);

	/**
	 * Retrieve the From: e-mail address from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the From: e-mail address
	 */
	protected abstract String getFrom(Message<?> message);

	/**
	 * Retrieve the Reply To: e-mail address from an integration message.
	 * 
	 * @param message the integration {@link Message}
	 * @return the ReplyTo: e-mail address
	 */
	protected abstract String getReplyTo(Message<?> message);

	/**
	 * Populate the mail message using the results of the template methods.
	 */
	public final void populateMailMessageHeader(MailMessage mailMessage, Message<?> message) {
		final String subject = getSubject(message);
		final String[] to = getTo(message);
		final String[] cc = getCc(message);
		final String[] bcc = getBcc(message);
		final String from = getFrom(message);
		final String replyTo = getReplyTo(message);
		if (subject != null) {
			mailMessage.setSubject(subject);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'SUBJECT' property available for mail message");
		}
		if (to != null) {
			mailMessage.setTo(to);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'TO' property available for mail message");
		}
		if (cc != null) {
			mailMessage.setCc(cc);
		}
		if (bcc != null) {
			mailMessage.setBcc(bcc);
		}
		if (from != null) {
			mailMessage.setFrom(from);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("no 'FROM' property available for mail message");
		}
		if (replyTo != null) {
			mailMessage.setReplyTo(replyTo);
		}
	}

}
