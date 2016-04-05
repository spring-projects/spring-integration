/*
 * Copyright 2002-2011 the original author or authors.
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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.springframework.util.Assert;

/**
 * A {@link MailReceiver} implementation that polls a mail server using the
 * POP3 protocol.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public class Pop3MailReceiver extends AbstractMailReceiver {

	public Pop3MailReceiver() {
		super();
		this.setProtocol("pop3");
	}

	public Pop3MailReceiver(String url) {
		super(url);
		if (url != null) {
			Assert.isTrue(url.startsWith("pop3"), "url must start with 'pop3'");
		}
		else {
			this.setProtocol("pop3");
		}
	}

	public Pop3MailReceiver(String host, String username, String password) {
		// -1 indicates default port
		this(host, -1, username, password);
	}

	public Pop3MailReceiver(String host, int port, String username, String password) {
		super(new URLName("pop3", host, port, "INBOX", username, password));
	}


	@Override
	protected Message[] searchForNewMessages() throws MessagingException {
		int messageCount = this.getFolder().getMessageCount();
		if (messageCount == 0) {
			return new Message[0];
		}
		return this.getFolder().getMessages();
	}

	/**
	 * Deletes the given messages from this receiver's folder, and closes it to expunge deleted messages.
	 *
	 * @param messages the messages to delete
	 * @throws MessagingException in case of JavaMail errors
	 */
	@Override
    protected void deleteMessages(Message[] messages) throws MessagingException {
		super.deleteMessages(messages);
		// expunge deleted mails, and make sure we've retrieved them before closing the folder
		for (int i = 0; i < messages.length; i++) {
			new MimeMessage((MimeMessage) messages[i]);
		}
	}
}
