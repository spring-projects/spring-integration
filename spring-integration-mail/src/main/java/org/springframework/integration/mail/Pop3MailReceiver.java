/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.URLName;
import jakarta.mail.internet.MimeMessage;

import org.springframework.util.Assert;

/**
 * A {@link MailReceiver} implementation that polls a mail server using the
 * POP3 protocol.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public class Pop3MailReceiver extends AbstractMailReceiver {

	public static final String PROTOCOL = "pop3";

	public Pop3MailReceiver() {
		setProtocol(PROTOCOL);
	}

	public Pop3MailReceiver(String url) {
		super(url);
		if (url != null) {
			Assert.isTrue(url.startsWith(PROTOCOL), "url must start with 'pop3'");
		}
		else {
			setProtocol(PROTOCOL);
		}
	}

	public Pop3MailReceiver(String host, String username, String password) {
		// -1 indicates default port
		this(host, -1, username, password);
	}

	public Pop3MailReceiver(String host, int port, String username, String password) {
		super(new URLName(PROTOCOL, host, port, "INBOX", username, password));
	}

	@Override
	protected Message[] searchForNewMessages() throws MessagingException {
		Folder folderToUse = getFolder();
		int messageCount = folderToUse.getMessageCount();
		if (messageCount == 0) {
			return new Message[0];
		}
		return folderToUse.getMessages();
	}

	/**
	 * Deletes the given messages from this receiver's folder, and closes it to expunge deleted messages.
	 * @param messages the messages to delete
	 * @throws MessagingException in case of JavaMail errors
	 */
	@Override
	protected void deleteMessages(Message[] messages) throws MessagingException {
		super.deleteMessages(messages);
		// expunge deleted mails, and make sure we've retrieved them before closing the folder
		for (Message message : messages) {
			new MimeMessage((MimeMessage) message);
		}
	}

}
