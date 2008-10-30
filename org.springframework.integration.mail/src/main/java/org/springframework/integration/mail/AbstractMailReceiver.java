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

import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.mail.monitor.MailTransportUtils;
import org.springframework.util.Assert;

/**
 * Base class for {@link MailReceiver} implementations.
 * 
 * @author Arjen Poutsma
 * @author Jonas Partner
 * @author Mark Fisher
 */
public abstract class AbstractMailReceiver implements MailReceiver, DisposableBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final URLName url;

	private volatile int maxFetchSize = -1;

	private volatile Session session;

	private volatile Store store;

	private volatile Folder folder;

	private volatile Properties javaMailProperties = new Properties();

	protected volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractMailReceiver(URLName urlName) {
		Assert.notNull(urlName, "urlName must not be null");
		this.url = urlName;
	}

	public AbstractMailReceiver(String url) {
		Assert.notNull(url, "url must not be null");
		this.url = new URLName(url);
	}


	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	protected Folder getFolder() {
		return this.folder;
	}

	/**
	 * Subclasses must implement this method to return new mail messages.
	 */
	protected abstract Message[] searchForNewMessages() throws MessagingException;

	/**
	 * Subclasses must implement this method to indicate whether the mail
	 * messages should be deleted after being received.
	 */
	protected abstract boolean shouldDeleteMessages();

	private void openSession() throws MessagingException {
		if (this.session == null) {
			this.session = Session.getInstance(this.javaMailProperties);
		}
		if (this.store == null) {
			this.store = this.session.getStore(this.url);
		}
		if (!this.store.isConnected()) {
			if (logger.isDebugEnabled()) {
				logger.debug("connecting to store [" + MailTransportUtils.toPasswordProtectedString(this.url) + "]");
			}
			this.store.connect();
		}
	}

	protected void openFolder() throws MessagingException {
		this.openSession();
		if (this.folder == null) {
			this.folder = this.store.getFolder(this.url);
		}
		if (this.folder == null || !this.folder.exists()) {
			throw new IllegalStateException("no such folder [" + this.url.getFile() + "]");
		}
		if (this.folder.isOpen()) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("opening folder [" + MailTransportUtils.toPasswordProtectedString(this.url) + "]");
		}
		if (this.shouldDeleteMessages()) {
			this.folder.open(Folder.READ_WRITE);
		}
		else {
			this.folder.open(Folder.READ_ONLY);
		}
	}

	public synchronized Message[] receive() {
		try {
			this.openFolder();
			if (logger.isInfoEnabled()) {
				logger.info("attempting to receive mail from folder [" + this.getFolder().getFullName() + "]");
			}
			Message[] messages = this.searchForNewMessages();
			if (this.maxFetchSize > 0 && messages.length > this.maxFetchSize) {
				Message[] reducedMessages = new Message[this.maxFetchSize];
				System.arraycopy(messages, 0, reducedMessages, 0, this.maxFetchSize);
				messages = reducedMessages;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("found " + messages.length + " new messages");
			}
			if (messages.length > 0) {
				this.fetchMessages(messages);
			}
			if (this.shouldDeleteMessages()) {
				this.deleteMessages(messages);
			}
			Message[] copiedMessages = new Message[messages.length];
			for (int i = 0; i < messages.length; i++) {
				copiedMessages[i] = new MimeMessage((MimeMessage) messages[i]);
			}
			return copiedMessages;
		}
		catch (Exception e) {
			throw new org.springframework.integration.core.MessagingException(
					"failure occurred while receiving from folder", e);
		}
		finally {
			MailTransportUtils.closeFolder(this.folder);
		}
	}

	/**
	 * Fetches the specified messages from this receiver's folder. Default
	 * implementation {@link Folder#fetch(Message[], FetchProfile) fetches}
	 * every {@link javax.mail.FetchProfile.Item}.
	 *
	 * @param messages the messages to fetch
	 * @throws MessagingException in case of JavMail errors
	 */
	protected void fetchMessages(Message[] messages) throws MessagingException {
		FetchProfile contentsProfile = new FetchProfile();
		contentsProfile.add(FetchProfile.Item.ENVELOPE);
		contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
		contentsProfile.add(FetchProfile.Item.FLAGS);
		this.folder.fetch(messages, contentsProfile);
	}

	/**
	 * Deletes the given messages from this receiver's folder. Only invoked
	 * when {@link #setDeleteMessages(boolean)} is <code>true</code>.
	 *
	 * @param messages the messages to delete
	 * @throws MessagingException in case of JavaMail errors
	 */
	protected void deleteMessages(Message[] messages) throws MessagingException {
		for (int i = 0; i < messages.length; i++) {
			messages[i].setFlag(Flags.Flag.DELETED, true);
		}
	}

	public void destroy() throws Exception {
		synchronized (this.initializationMonitor) {
			MailTransportUtils.closeFolder(this.folder);
			MailTransportUtils.closeService(this.store);
			this.folder = null;
			this.store = null;
			this.initialized = false;
		}
	}

	@Override
	public String toString() {
		return this.url.toString();
	}

}
