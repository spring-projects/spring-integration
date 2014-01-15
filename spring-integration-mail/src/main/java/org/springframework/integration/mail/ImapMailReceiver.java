/*
 * Copyright 2002-2014 the original author or authors.
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
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

import org.springframework.util.Assert;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

/**
 * A {@link MailReceiver} implementation for receiving mail messages from a
 * mail server that supports the IMAP protocol. In addition to the pollable
 * {@link #receive()} method, the {@link #waitForNewMessages()} method provides
 * the option of blocking until new messages are available prior to calling
 * {@link #receive()}. That option is only available if the server supports
 * the {@link IMAPFolder#idle() idle} command.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class ImapMailReceiver extends AbstractMailReceiver {

	private volatile boolean shouldMarkMessagesAsRead = true;

	private volatile SearchTermStrategy searchTermStrategy = new DefaultSearchTermStrategy();

	private final MessageCountListener messageCountListener = new SimpleMessageCountListener();

	public ImapMailReceiver() {
		super();
		this.setProtocol("imap");
	}

	public ImapMailReceiver(String url) {
		super(url);
		if (url != null) {
			Assert.isTrue(url.toLowerCase().startsWith("imap"),
					"URL must start with 'imap' for the IMAP Mail receiver.");
		}
		else {
			this.setProtocol("imap");
		}
	}


	/**
	 * Check if messages should be marked as read.
	 *
	 * @return true if messages should be marked as read.
	 */
	public Boolean isShouldMarkMessagesAsRead() {
		return shouldMarkMessagesAsRead;
	}

	/**
	 * Provides a way to set custom {@link SearchTermStrategy} to compile a {@link SearchTerm}
	 * to be applied when retrieving mail
	 *
	 * @param searchTermStrategy The search term strategy implementation.
	 */
	public void setSearchTermStrategy(SearchTermStrategy searchTermStrategy) {
		Assert.notNull(searchTermStrategy, "'searchTermStrategy' must not be null");
		this.searchTermStrategy = searchTermStrategy;
	}

	/**
	 * Specify if messages should be marked as read.
	 *
	 * @param shouldMarkMessagesAsRead true if messages should be marked as read.
	 */
	public void setShouldMarkMessagesAsRead(Boolean shouldMarkMessagesAsRead) {
		this.shouldMarkMessagesAsRead = shouldMarkMessagesAsRead;
	}

	/**
	 * This method is unique to the IMAP receiver and only works if IMAP IDLE
	 * is supported (see RFC 2177 for more detail).
	 *
	 * @throws MessagingException Any MessagingException.
	 */
	public void waitForNewMessages() throws MessagingException {
		this.openFolder();
		Folder folder = this.getFolder();
		Assert.state(folder instanceof IMAPFolder,
				"folder is not an instance of [" + IMAPFolder.class.getName() + "]");
		IMAPFolder imapFolder = (IMAPFolder) folder;
		if (imapFolder.hasNewMessages()) {
			return;
		}
		else if (!folder.getPermanentFlags().contains(Flags.Flag.RECENT)) {
			if (searchForNewMessages().length > 0) {
				return;
			}
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
		SearchTerm searchTerm = this.compileSearchTerms(supportedFlags);
		Folder folder = this.getFolder();
		if (folder.isOpen()) {
			Message[] messages = searchTerm != null ? folder.search(searchTerm) : folder.getMessages();
			for (Message message : messages) {
				((IMAPMessage) message).setPeek(true);
			}
			return messages;
		}
		throw new MessagingException("Folder is closed");
	}

	private SearchTerm compileSearchTerms(Flags supportedFlags) {
		return this.searchTermStrategy.generateSearchTerm(supportedFlags, this.getFolder());
	}

	@Override
	protected void setAdditionalFlags(Message message) throws MessagingException {
		super.setAdditionalFlags(message);
		if (this.shouldMarkMessagesAsRead) {
			message.setFlag(Flag.SEEN, true);
		}
	}


	/**
	 * Callback used for handling the event-driven idle response.
	 */
	private static class SimpleMessageCountListener extends MessageCountAdapter {

		@Override
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

	private class DefaultSearchTermStrategy implements SearchTermStrategy {

		@Override
		public SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder) {
			SearchTerm searchTerm = null;
			boolean recentFlagSupported = false;
			if (supportedFlags != null) {
				recentFlagSupported = supportedFlags.contains(Flags.Flag.RECENT);
				if (recentFlagSupported) {
					searchTerm = new FlagTerm(new Flags(Flags.Flag.RECENT), true);
				}
				if (supportedFlags.contains(Flags.Flag.ANSWERED)) {
					NotTerm notAnswered = new NotTerm(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
					if (searchTerm == null) {
						searchTerm = notAnswered;
					}
					else {
						searchTerm = new AndTerm(searchTerm, notAnswered);
					}
				}
				if (supportedFlags.contains(Flags.Flag.DELETED)) {
					NotTerm notDeleted = new NotTerm(new FlagTerm(new Flags(Flags.Flag.DELETED), true));
					if (searchTerm == null) {
						searchTerm = notDeleted;
					}
					else {
						searchTerm = new AndTerm(searchTerm, notDeleted);
					}
				}
				if (supportedFlags.contains(Flags.Flag.SEEN)) {
					NotTerm notSeen = new NotTerm(new FlagTerm(new Flags(Flags.Flag.SEEN), true));
					if (searchTerm == null) {
						searchTerm = notSeen;
					}
					else {
						searchTerm = new AndTerm(searchTerm, notSeen);
					}
				}
			}

			if (!recentFlagSupported) {
				NotTerm notFlagged = null;
				if (folder.getPermanentFlags().contains(Flags.Flag.USER)) {
					logger.debug("This email server does not support RECENT flag, but it does support " +
							"USER flags which will be used to prevent duplicates during email fetch.");
					Flags siFlags = new Flags();
					siFlags.add(AbstractMailReceiver.SI_USER_FLAG);
					notFlagged = new NotTerm(new FlagTerm(siFlags, true));
				}
				else {
					logger.debug("This email server does not support RECENT or USER flags. " +
							"System flag 'Flag.FLAGGED' will be used to prevent duplicates during email fetch.");
					notFlagged = new NotTerm(new FlagTerm(new Flags(Flags.Flag.FLAGGED), true));
				}
				if (searchTerm == null) {
					searchTerm = notFlagged;
				}
				else {
					searchTerm = new AndTerm(searchTerm, notFlagged);
				}
			}
			return searchTerm;
		}

	}


}
