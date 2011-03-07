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

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

import org.springframework.scheduling.TaskScheduler;
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
 */
public class ImapMailReceiver extends AbstractMailReceiver {
	private volatile boolean shouldMarkMessagesAsRead = true;;
	private final MessageCountListener messageCountListener = new SimpleMessageCountListener();
	
	private volatile long connectionPingInterval = 10000;


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
	 * Check if messages should be marked as read
	 */
	public Boolean isShouldMarkMessagesAsRead() {
		return shouldMarkMessagesAsRead;
	}
	/**
	 * Specify is messages should be marked as read
	 */
	public void setShouldMarkMessagesAsRead(Boolean shouldMarkMessagesAsRead) {
		this.shouldMarkMessagesAsRead = shouldMarkMessagesAsRead;
	}
	/**
	 * This method is unique to the IMAP receiver and only works if IMAP IDLE
	 * is supported (see RFC 2177 for more detail).
	 */
	public void waitForNewMessages() throws MessagingException{
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
		SearchTerm searchTerm = this.compileSearchTerms(supportedFlags);
		Folder folder = this.getFolder();
		if (folder.isOpen()){
			Message[] messages = searchTerm != null ? folder.search(searchTerm) : folder.getMessages();
			for (Message message : messages) {
				((IMAPMessage)message).setPeek(true);
			}
			return messages;
		}
		throw new MessagingException("Folder is closed");
	}
	
	private SearchTerm compileSearchTerms(Flags supportedFlags){
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
		NotTerm notDeleted = new NotTerm( new FlagTerm(new Flags(Flags.Flag.DELETED), true) );
		NotTerm notSeen = new NotTerm( new FlagTerm(new Flags(Flags.Flag.SEEN), true) );
		if (searchTerm == null){
			searchTerm = notDeleted;
		} 
		
		if (this.getFolder().getPermanentFlags().contains(Flags.Flag.USER)){
			Flags siFlags = new Flags();
			siFlags.add("spring-integration");
			searchTerm = new AndTerm(new SearchTerm[]{searchTerm, notSeen, new FlagTerm(siFlags, false)});
		}
		else {
			searchTerm = new AndTerm(new SearchTerm[]{searchTerm, notSeen, new FlagTerm(new Flags(Flags.Flag.FLAGGED), false)});
		}
		
		
		return searchTerm;
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
	
	@Override
	protected void onInit() throws Exception {
		super.onInit();
//		if (this.shouldMarkMessagesAsRead){
//			this.folderOpenMode = Folder.READ_WRITE;
//		}
		this.initialized = true;
		TaskScheduler scheduler = this.getTaskScheduler();
		if (scheduler != null){
			scheduler.scheduleAtFixedRate(new Runnable() {	
				public void run() {
					try {
						Store store = getStore();
						if (initialized && store != null){
							store.isConnected();
						}		
					} 
					catch (Throwable ignore) {
					}
				}
			}, connectionPingInterval);
		}
	}
	/**
	 * 
	 */
	protected void setAdditionalFlags(Message message) throws MessagingException{
		super.setAdditionalFlags(message);
		if (this.shouldMarkMessagesAsRead) {
			message.setFlag(Flag.SEEN, true);
		}
	}
}
