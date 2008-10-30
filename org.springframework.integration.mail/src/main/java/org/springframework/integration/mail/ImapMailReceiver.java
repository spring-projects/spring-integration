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

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

import org.springframework.integration.mail.monitor.MailTransportUtils;
import org.springframework.util.Assert;

import com.sun.mail.imap.IMAPFolder;

/**
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public class ImapMailReceiver extends AbstractMailReceiver {

	private volatile boolean shouldDeleteMessages = true;

	private final MessageCountListener messageCountListener = new SimpleMessageCountListener();


	public ImapMailReceiver(String url) {
		super(url);
	}


	/**
	 * Specify whether mail messages should be deleted after retrieval.
	 * The default is <code>true</code>. 
	 */
	public void setShouldDeleteMessages(boolean shouldDeleteMessages) {
		this.shouldDeleteMessages = shouldDeleteMessages;
	}

	@Override
	protected boolean shouldDeleteMessages() {
		return this.shouldDeleteMessages;
	}

	/**
	 * This method is unique to the IMAP receiver and only works if IMAP IDLE
	 * is supported (see RFC 2177 for more detail).
	 */
	public void waitForNewMessages() throws MessagingException, InterruptedException {
		this.openFolder();
		Assert.state(this.getFolder() instanceof IMAPFolder,
				"folder is not an instance of [" + IMAPFolder.class.getName() + "]");
		IMAPFolder imapFolder = (IMAPFolder) this.getFolder();
		if (imapFolder.hasNewMessages()) {
			return;
		}
		imapFolder.addMessageCountListener(this.messageCountListener);
		try {
			imapFolder.idle();
		}
		finally {
			imapFolder.removeMessageCountListener(this.messageCountListener);
		}
	}

	/**
	 * Retrieves new messages from this receiver's folder. This implementation
	 * creates a {@link SearchTerm} that searches for all messages in the
	 * folder that are {@link javax.mail.Flags.Flag#RECENT RECENT}, not
	 * {@link javax.mail.Flags.Flag#ANSWERED ANSWERED}, and not
	 * {@link javax.mail.Flags.Flag#DELETED DELETED}. The search term is used
	 * to {@link Folder#search(SearchTerm) search} for new messages.
	 *
	 * @return the new messages
	 * @throws MessagingException in case of JavaMail errors
	 */
	@Override
	protected Message[] searchForNewMessages() throws MessagingException {
		Flags supportedFlags = this.getFolder().getPermanentFlags();
		SearchTerm searchTerm = null;
		if (supportedFlags != null) {
			if (supportedFlags.contains(Flags.Flag.RECENT)) {
				searchTerm = new FlagTerm(new Flags(Flags.Flag.RECENT), true);
			}
			if (supportedFlags.contains(Flags.Flag.ANSWERED)) {
				FlagTerm answeredTerm = new FlagTerm(new Flags(Flags.Flag.ANSWERED), false);
				if (searchTerm == null) {
					searchTerm = answeredTerm;
				}
				else {
					searchTerm = new AndTerm(searchTerm, answeredTerm);
				}
			}
			if (supportedFlags.contains(Flags.Flag.DELETED)) {
				FlagTerm deletedTerm = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
				if (searchTerm == null) {
					searchTerm = deletedTerm;
				}
				else {
					searchTerm = new AndTerm(searchTerm, deletedTerm);
				}
			}
		}
		Message[] results = searchTerm != null ? this.getFolder().search(searchTerm) : this.getFolder().getMessages();
		if (results == null || results.length == 0) {
			MailTransportUtils.closeFolder(this.getFolder());
		}
		return results;
	}


	/**
	 * Callback used for handling the event-driven idle response.
	 */
	private static class SimpleMessageCountListener extends MessageCountAdapter {

		public void messagesAdded(MessageCountEvent event) {
			Message[] messages = event.getMessages();
			for (Message message : messages) {
				try {
					// this will return the flow to the idle call
					message.getLineCount();
				}
				catch (MessagingException e) {
					// ignored;
				}
			}
		}
	}

}
