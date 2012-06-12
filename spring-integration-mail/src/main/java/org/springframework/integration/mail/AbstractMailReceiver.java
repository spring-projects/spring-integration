/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
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
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.util.Assert;

/**
 * Base class for {@link MailReceiver} implementations.
 *
 * @author Arjen Poutsma
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractMailReceiver extends IntegrationObjectSupport implements MailReceiver, DisposableBean{

	public final static String SI_USER_FLAG = "spring-integration-mail-adapter";

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final URLName url;

	private volatile String protocol;

	private volatile int maxFetchSize = -1;

	private volatile Session session;

	private volatile Store store;

	private final ThreadLocal<MailReceiverContext> contextHolder = new ThreadLocal<MailReceiverContext>();

	private volatile boolean shouldDeleteMessages;

	protected volatile int folderOpenMode = Folder.READ_ONLY;

	private volatile Properties javaMailProperties = new Properties();

	private volatile Authenticator javaMailAuthenticator;

	private final StandardEvaluationContext context = new StandardEvaluationContext();

	private volatile Expression selectorExpression;

	protected volatile boolean initialized;


	public AbstractMailReceiver() {
		this.url = null;
	}

	public AbstractMailReceiver(URLName urlName) {
		Assert.notNull(urlName, "urlName must not be null");
		this.url = urlName;
	}

	public AbstractMailReceiver(String url) {
		if (url != null) {
			this.url = new URLName(url);
		}
		else {
			this.url = null;
		}
	}

	public void setSelectorExpression(Expression selectorExpression) {
		this.selectorExpression = selectorExpression;
	}

	public void setProtocol(String protocol) {
		if (this.url != null) {
			Assert.isTrue(this.url.getProtocol().equals(protocol),
					"The 'protocol' does not match that provided by the Store URI.");
		}
		this.protocol = protocol;
	}

	/**
	 * Set the {@link Session}. Otherwise, the Session will be created by invocation of
	 * {@link Session#getInstance(Properties)} or {@link Session#getInstance(Properties, Authenticator)}.
	 *
	 * @see #setJavaMailProperties(Properties)
	 * @see #setJavaMailAuthenticator(Authenticator)
	 */
	public void setSession(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * A new {@link Session} will be created with these properties (and the JavaMailAuthenticator if provided).
	 * Use either this method or {@link #setSession}, but not both.
	 *
	 * @see #setJavaMailAuthenticator(Authenticator)
	 * @see #setSession(Session)
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	/**
	 * Optional, sets the Authenticator to be used to obtain a session. This will not be used if
	 * {@link AbstractMailReceiver#setSession} has been used to configure the {@link Session} directly.
	 *
	 * @see #setSession(Session)
	 */
	public void setJavaMailAuthenticator(Authenticator javaMailAuthenticator) {
		this.javaMailAuthenticator = javaMailAuthenticator;
	}

	/**
	 * Specify the maximum number of Messages to fetch per call to {@link #receive()}.
	 */
	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	/**
	 * Specify whether mail messages should be deleted after retrieval.
	 */
	public void setShouldDeleteMessages(boolean shouldDeleteMessages) {
		this.shouldDeleteMessages = shouldDeleteMessages;
	}
	/**
	 * Indicates whether the mail messages should be deleted after being received.
	 */
	protected boolean shouldDeleteMessages() {
		return this.shouldDeleteMessages;
	}

	public Folder getFolder() {
		if (this.contextHolder.get() == null) {
			try {
				this.openFolder();
			}
			catch (MessagingException e) {
				throw new org.springframework.integration.MessagingException("Failed to open folder", e);
			}
		}
		return this.contextHolder.get().getFolder();
	}

	public MailReceiverContext getTransactionContext() {
		if (this.contextHolder.get() == null) {
			this.getFolder();
		}
		return this.contextHolder.get();
	}

	/**
	 * Subclasses must implement this method to return new mail messages.
	 */
	protected abstract Message[] searchForNewMessages() throws MessagingException;

	private void openSession() throws MessagingException {
		if (this.session == null) {
			if (this.javaMailAuthenticator != null) {
				this.session = Session.getInstance(this.javaMailProperties, this.javaMailAuthenticator);
			}
			else {
				this.session = Session.getInstance(this.javaMailProperties);
			}
		}
		if (this.store == null) {
			if (this.url != null) {
				this.store = this.session.getStore(this.url);
			}
			else if (this.protocol != null) {
				this.store = this.session.getStore(this.protocol);
			}
			else {
				this.store = this.session.getStore();
			}
		}
		if (!this.store.isConnected()) {
			if (logger.isDebugEnabled()) {
				logger.debug("connecting to store [" + MailTransportUtils.toPasswordProtectedString(this.url) + "]");
			}
			this.store.connect();
		}
	}

	protected synchronized void openFolder() throws MessagingException {
		this.openSession();
		MailReceiverContext context = this.contextHolder.get();
		Folder folder = null;
		if (context == null) {
			folder = this.store.getFolder(this.url);
			this.contextHolder.set(new MailReceiverContext(folder));
		}
		else {
			folder = context.getFolder();
		}
		if (folder == null || !folder.exists()) {
			throw new IllegalStateException("no such folder [" + this.url.getFile() + "]");
		}
		if (folder.isOpen()) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("opening folder [" + MailTransportUtils.toPasswordProtectedString(this.url) + "]");
		}
		folder.open(this.folderOpenMode);
	}

	public Message[] receive() throws javax.mail.MessagingException {
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
		List<Message> copiedMessages = new LinkedList<Message>();
		logger.debug("Recieved " + messages.length + " messages");

		for (int i = 0; i < messages.length; i++) {
			if (this.selectorExpression != null) {
				Message message = messages[i];
				if (this.selectorExpression.getValue(this.context, message, Boolean.class)){
					copiedMessages.add(new MimeMessage((MimeMessage) message));
				}
			}
			else {
				copiedMessages.add(new MimeMessage((MimeMessage) messages[i]));
			}
		}
		if (messages.length > 0) {
			this.contextHolder.get().setMessages(messages);
		}
		return copiedMessages.toArray(new Message[copiedMessages.size()]);
	}

	/**
	 * Fetches the specified messages from this receiver's folder. Default
	 * implementation {@link Folder#fetch(Message[], FetchProfile) fetches}
	 * every {@link javax.mail.FetchProfile.Item}.
	 *
	 * @param messages the messages to fetch
	 * @throws MessagingException in case of JavaMail errors
	 */
	protected void fetchMessages(Message[] messages) throws MessagingException {
		FetchProfile contentsProfile = new FetchProfile();
		contentsProfile.add(FetchProfile.Item.ENVELOPE);
		contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
		contentsProfile.add(FetchProfile.Item.FLAGS);
		this.contextHolder.get().getFolder().fetch(messages, contentsProfile);
	}

	/**
	 * Deletes the given messages from this receiver's folder.
	 *
	 * @param messages the messages to delete
	 * @throws MessagingException in case of JavaMail errors
	 */
	protected void deleteMessages(Message[] messages) throws MessagingException {
		for (int i = 0; i < messages.length; i++) {
			messages[i].setFlag(Flags.Flag.DELETED, true);
		}
	}

	/**
	 * Optional method allowing you to set additional flags.
	 * Currently only implemented in IMapMailReceiver.
	 *
	 * @param message
	 * @throws MessagingException
	 */
	protected void setAdditionalFlags(Message message) throws MessagingException {
	}

	public synchronized void destroy() throws Exception {
		MailTransportUtils.closeFolder(this.contextHolder.get().getFolder(), this.shouldDeleteMessages);
		MailTransportUtils.closeService(this.store);
		this.store = null;
		this.initialized = false;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.folderOpenMode = Folder.READ_WRITE;
		this.initialized = true;
	}

	@Override
	public String toString() {
		return this.url.toString();
	}

	Store getStore() {
		return this.store;
	}

	/**
	 * Delete and expunge messages after success.
	 * @param folder
	 */
	public void closeContextAfterSuccess(MailReceiverContext context) {
		Assert.notNull(context, "Mail Reader Context cannot be null");
		Message[] messages = this.contextHolder.get().getMessages();
		Assert.state(messages != null, "No messages in mail receiver context");
		RuntimeException exceptionToThrow = null;
		boolean recentFlagSupported = false;

		Flags flags = context.getFolder().getPermanentFlags();

		if (flags != null){
			recentFlagSupported = flags.contains(Flags.Flag.RECENT);
		}

		for (int i = 0; i < messages.length; i++) {
			try {
				if (!recentFlagSupported){
					if (flags != null && flags.contains(Flags.Flag.USER)){
						if (logger.isDebugEnabled()){
							logger.debug("USER flags are supported by this mail server. Flagging message with '" + SI_USER_FLAG + "' user flag");
						}
						Flags siFlags = new Flags();
						siFlags.add(SI_USER_FLAG);
						messages[i].setFlags(siFlags, true);
					}
					else {
						if (logger.isDebugEnabled()){
							logger.debug("USER flags are not supported by this mail server. Flagging message with system flag");
						}
						messages[i].setFlag(Flags.Flag.FLAGGED, true);
					}
				}
				if (this.selectorExpression != null) {
					Message message = messages[i];
					if (this.selectorExpression.getValue(this.context, message, Boolean.class)){
						this.setAdditionalFlags(message);
					}
					else {
						if (logger.isDebugEnabled()){
							logger.debug("Fetched email with subject '" + message.getSubject() + "' will be discarded by the matching filter" +
											" and will not be flagged as SEEN.");
						}
					}
				}
				else {
					this.setAdditionalFlags(messages[i]);
				}
			}
			catch (Exception e) {
				exceptionToThrow = new org.springframework.integration.MessagingException("Failed to set flags", e);
			}
		}

		if (this.shouldDeleteMessages) {
			if (messages != null) {
				try {
					this.deleteMessages(messages);
				}
				catch (MessagingException e) {
					exceptionToThrow = new org.springframework.integration.MessagingException("Failed to delete messages", e);
				}
			}
		}
		Folder folder = context.getFolder();
		MailTransportUtils.closeFolder(folder, this.shouldDeleteMessages);
		this.contextHolder.set(null);
		if (exceptionToThrow != null) {
			throw exceptionToThrow;
		}
	}

	public void closeContextAfterFailure(MailReceiverContext context) {
		Assert.notNull(context, "Mail Reader Context cannot be null");
		MailTransportUtils.closeFolder(context.getFolder(), false);
		this.contextHolder.set(null);
	}

}
