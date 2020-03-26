/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Base class for {@link MailReceiver} implementations.
 *
 * @author Arjen Poutsma
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractMailReceiver extends IntegrationObjectSupport implements MailReceiver, DisposableBean {

	/**
	 * Default user flag for marking messages as seen by this receiver:
	 * {@value #DEFAULT_SI_USER_FLAG}.
	 */
	public static final String DEFAULT_SI_USER_FLAG = "spring-integration-mail-adapter";

	private final URLName url;

	private final ReentrantReadWriteLock folderLock = new ReentrantReadWriteLock();

	private final Lock folderReadLock = this.folderLock.readLock();

	private final Lock folderWriteLock = this.folderLock.writeLock();

	private String protocol;

	private int maxFetchSize = -1;

	private Session session;

	private boolean shouldDeleteMessages;

	private int folderOpenMode = Folder.READ_ONLY;

	private Properties javaMailProperties = new Properties();

	private Authenticator javaMailAuthenticator;

	private StandardEvaluationContext evaluationContext;

	private Expression selectorExpression;

	private HeaderMapper<MimeMessage> headerMapper;

	private String userFlag = DEFAULT_SI_USER_FLAG;

	private boolean embeddedPartsAsBytes = true;

	private boolean simpleContent;

	private boolean autoCloseFolder = true;

	private volatile Store store;

	private volatile Folder folder;

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
	 * @param session The session.
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
	 * @param javaMailProperties The javamail properties.
	 * @see #setJavaMailAuthenticator(Authenticator)
	 * @see #setSession(Session)
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	protected Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * Optional, sets the Authenticator to be used to obtain a session. This will not be used if
	 * {@link AbstractMailReceiver#setSession} has been used to configure the {@link Session} directly.
	 * @param javaMailAuthenticator The javamail authenticator.
	 * @see #setSession(Session)
	 */
	public void setJavaMailAuthenticator(Authenticator javaMailAuthenticator) {
		this.javaMailAuthenticator = javaMailAuthenticator;
	}

	/**
	 * Specify the maximum number of Messages to fetch per call to {@link #receive()}.
	 * @param maxFetchSize The max fetch size.
	 */
	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	/**
	 * Specify whether mail messages should be deleted after retrieval.
	 * @param shouldDeleteMessages true to delete messages.
	 */
	public void setShouldDeleteMessages(boolean shouldDeleteMessages) {
		this.shouldDeleteMessages = shouldDeleteMessages;
	}

	/**
	 * Indicates whether the mail messages should be deleted after being received.
	 * @return true when messages will be deleted.
	 */
	protected boolean shouldDeleteMessages() {
		return this.shouldDeleteMessages;
	}

	protected String getUserFlag() {
		return this.userFlag;
	}

	/**
	 * Set the name of the flag to use to flag messages when the server does
	 * not support \Recent but supports user flags; default {@value #DEFAULT_SI_USER_FLAG}.
	 * @param userFlag the flag.
	 * @since 4.2.2
	 */
	public void setUserFlag(String userFlag) {
		this.userFlag = userFlag;
	}

	/**
	 * Set the header mapper; if a header mapper is not provided, the message payload is
	 * a {@link MimeMessage}, when provided, the headers are mapped and the payload is
	 * the {@link MimeMessage} content.
	 * @param headerMapper the header mapper.
	 * @since 4.3
	 * @see #setEmbeddedPartsAsBytes(boolean)
	 */
	public void setHeaderMapper(HeaderMapper<MimeMessage> headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * When a header mapper is provided determine whether an embedded {@link Part} (e.g
	 * {@link Message} or {@link Multipart} content is rendered as a byte[] in the
	 * payload. Otherwise, leave as a {@link Part}. These objects are not suitable for
	 * downstream serialization. Default: true.
	 * <p>This has no effect if there is no header mapper, in that case the payload is the
	 * {@link MimeMessage}.
	 * @param embeddedPartsAsBytes the embeddedPartsAsBytes to set.
	 * @since 4.3
	 * @see #setHeaderMapper(HeaderMapper)
	 */
	public void setEmbeddedPartsAsBytes(boolean embeddedPartsAsBytes) {
		this.embeddedPartsAsBytes = embeddedPartsAsBytes;
	}

	/**
	 * {@link MimeMessage#getContent()} returns just the email body.
	 * <pre class="code">
	 * foo
	 * </pre>
	 * Some subclasses, such as {@code IMAPMessage} return some headers with the body.
	 * <pre class="code">
	 * To: foo@bar
	 * From: bar@baz
	 * Subject: Test Email
	 *
	 *  foo
	 * </pre>
	 * Starting with version 5.0, messages emitted by mail receivers will render the
	 * content in the same way as the {@link MimeMessage} implementation returned by
	 * javamail. In versions 2.2 through 4.3, the content was always just the body,
	 * regardless of the underlying message type (unless a header mapper was provided,
	 * in which case the payload was rendered by the underlying {@link MimeMessage}.
	 * <p>To revert to the previous behavior, set this flag to true. In addition, even
	 * if a header mapper is provided, the payload will just be the email body.
	 * @param simpleContent true to render simple content.
	 * @since 5.0
	 */
	public void setSimpleContent(boolean simpleContent) {
		this.simpleContent = simpleContent;
	}

	/**
	 * Configure a {@code boolean} flag to close the folder automatically after a fetch (default) or
	 * populate an additional {@link IntegrationMessageHeaderAccessor#CLOSEABLE_RESOURCE} message header instead.
	 * It is the downstream flow's responsibility to obtain this header and call its {@code close()} whenever
	 * it is necessary.
	 * <p> Keeping the folder open is useful in cases where communication with the server is needed
	 * when parsing multipart content of the email with attachments.
	 * <p> The {@link #setSimpleContent(boolean)} and {@link #setHeaderMapper(HeaderMapper)} options are not
	 * affected by this flag.
	 * @param autoCloseFolder {@code false} do not close the folder automatically after a fetch.
	 * @since 5.2
	 */
	public void setAutoCloseFolder(boolean autoCloseFolder) {
		this.autoCloseFolder = autoCloseFolder;
	}

	protected Folder getFolder() {
		return this.folder;
	}

	protected int getFolderOpenMode() {
		return this.folderOpenMode;
	}

	/**
	 * Subclasses must implement this method to return new mail messages.
	 * @return An array of messages.
	 * @throws MessagingException Any MessagingException.
	 */
	protected abstract Message[] searchForNewMessages() throws MessagingException;

	private void openSession() {
		if (this.session == null) {
			if (this.javaMailAuthenticator != null) {
				this.session = Session.getInstance(this.javaMailProperties, this.javaMailAuthenticator);
			}
			else {
				this.session = Session.getInstance(this.javaMailProperties);
			}
		}
	}

	private void connectStoreIfNecessary() throws MessagingException {
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
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("connecting to store [" + this.store.getURLName() + "]");
			}
			this.store.connect();
		}
	}

	protected void openFolder() throws MessagingException {
		if (this.folder == null) {
			openSession();
			connectStoreIfNecessary();
			this.folder = obtainFolderInstance();
		}
		else {
			connectStoreIfNecessary();
		}
		if (this.folder == null || !this.folder.exists()) {
			throw new IllegalStateException("no such folder [" + this.url.getFile() + "]");
		}
		if (this.folder.isOpen()) {
			return;
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("opening folder [" + this.folder.getURLName() + "]");
		}
		this.folder.open(this.folderOpenMode);
	}

	private Folder obtainFolderInstance() throws MessagingException {
		return this.store.getFolder(this.url);
	}

	@Override
	public Object[] receive() throws javax.mail.MessagingException {
		this.folderReadLock.lock(); // NOSONAR - guarded with the getReadHoldCount()
		try {
			try {
				Folder folderToCheck = getFolder();
				if (folderToCheck == null || !folderToCheck.isOpen()) {
					this.folderReadLock.unlock();
					this.folderWriteLock.lock();
					try {
						openFolder();
						this.folderReadLock.lock();
					}
					finally {
						this.folderWriteLock.unlock();
					}
				}
				return convertMessagesIfNecessary(searchAndFilterMessages());
			}
			finally {
				if (this.autoCloseFolder) {
					closeFolder();
				}
			}
		}
		finally {
			if (this.folderLock.getReadHoldCount() > 0) {
				this.folderReadLock.unlock();
			}
		}
	}

	protected void closeFolder() {
		this.folderReadLock.lock();
		try {
			MailTransportUtils.closeFolder(this.folder, this.shouldDeleteMessages);
		}
		finally {
			this.folderReadLock.unlock();
		}
	}

	private MimeMessage[] searchAndFilterMessages() throws MessagingException {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("attempting to receive mail from folder [" + this.folder.getFullName() + "]");
		}
		Message[] messages = searchForNewMessages();
		if (this.maxFetchSize > 0 && messages.length > this.maxFetchSize) {
			Message[] reducedMessages = new Message[this.maxFetchSize];
			System.arraycopy(messages, 0, reducedMessages, 0, this.maxFetchSize);
			messages = reducedMessages;
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("found " + messages.length + " new messages");
		}
		if (messages.length > 0) {
			fetchMessages(messages);
		}

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Received " + messages.length + " messages");
		}

		MimeMessage[] filteredMessages = filterMessagesThruSelector(messages);

		postProcessFilteredMessages(filteredMessages);
		return filteredMessages;
	}

	private Object[] convertMessagesIfNecessary(MimeMessage[] filteredMessages) {
		if (this.headerMapper != null || !this.autoCloseFolder) {
			org.springframework.messaging.Message<?>[] converted =
					new org.springframework.messaging.Message<?>[filteredMessages.length];
			int n = 0;
			for (MimeMessage message : filteredMessages) {
				Object payload = message;
				Map<String, Object> headers = null;
				if (this.headerMapper != null) {
					headers = this.headerMapper.toHeaders(message);
					payload = extractContent(message, headers);
				}
				AbstractIntegrationMessageBuilder<Object> messageBuilder =
						getMessageBuilderFactory()
								.withPayload(payload)
								.copyHeaders(headers);
				if (!this.autoCloseFolder) {
					messageBuilder.setHeader(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE,
							(Closeable) this::closeFolder);
				}
				converted[n++] = messageBuilder.build();
			}
			return converted;
		}
		else {
			return filteredMessages;
		}
	}

	private Object extractContent(MimeMessage message, Map<String, Object> headers) { // NOSONAR
		Object content;
		try {
			MimeMessage theMessage = message;
			if (this.simpleContent) {
				theMessage = new IntegrationMimeMessage(message);
			}
			content = theMessage.getContent();
			if (content instanceof String) {
				headers.put(MessageHeaders.CONTENT_TYPE, "text/plain");
				String mailContentType = (String) headers.get(MailHeaders.CONTENT_TYPE);
				if (mailContentType != null && mailContentType.toLowerCase().startsWith("text")) {
					headers.put(MessageHeaders.CONTENT_TYPE, mailContentType);
				}
			}
			else if (content instanceof InputStream) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				FileCopyUtils.copy((InputStream) content, baos);
				content = byteArrayToContent(headers, baos);
			}
			else if (content instanceof Multipart && this.embeddedPartsAsBytes) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				((Multipart) content).writeTo(baos);
				content = byteArrayToContent(headers, baos);
			}
			else if (content instanceof Part && this.embeddedPartsAsBytes) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				((Part) content).writeTo(baos);
				content = byteArrayToContent(headers, baos);
			}
			return content;
		}
		catch (Exception e) {
			throw new org.springframework.messaging.MessagingException("Failed to extract content from " + message, e);
		}
	}

	private Object byteArrayToContent(Map<String, Object> headers, ByteArrayOutputStream baos) {
		headers.put(MessageHeaders.CONTENT_TYPE, "application/octet-stream");
		return baos.toByteArray();
	}

	private void postProcessFilteredMessages(Message[] filteredMessages) throws MessagingException {
		setMessageFlags(filteredMessages);

		if (shouldDeleteMessages()) {
			deleteMessages(filteredMessages);
		}
		if (this.headerMapper == null) {
			// Copy messages to cause an eager fetch
			for (int i = 0; i < filteredMessages.length; i++) {
				MimeMessage mimeMessage = new IntegrationMimeMessage((MimeMessage) filteredMessages[i]);
				filteredMessages[i] = mimeMessage;
			}
		}
	}

	private void setMessageFlags(Message[] filteredMessages) throws MessagingException {
		boolean recentFlagSupported = false;

		Flags flags = getFolder().getPermanentFlags();

		if (flags != null) {
			recentFlagSupported = flags.contains(Flags.Flag.RECENT);
		}
		for (Message message : filteredMessages) {
			if (!recentFlagSupported) {
				if (flags != null && flags.contains(Flags.Flag.USER)) {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("USER flags are supported by this mail server. Flagging message with '"
								+ this.userFlag + "' user flag");
					}
					Flags siFlags = new Flags();
					siFlags.add(this.userFlag);
					message.setFlags(siFlags, true);
				}
				else {
					this.logger.debug("USER flags are not supported by this mail server. " +
							"Flagging message with system flag");
					message.setFlag(Flags.Flag.FLAGGED, true);
				}
			}
			setAdditionalFlags(message);
		}
	}

	/**
	 * Will filter Messages thru selector. Messages that did not pass selector filtering criteria
	 * will be filtered out and remain on the server as never touched.
	 */
	private MimeMessage[] filterMessagesThruSelector(Message[] messages) throws MessagingException {
		List<MimeMessage> filteredMessages = new LinkedList<>();
		for (Message message1 : messages) {
			MimeMessage message = (MimeMessage) message1;
			if (this.selectorExpression != null) {
				if (Boolean.TRUE.equals(
						this.selectorExpression.getValue(this.evaluationContext, message, Boolean.class))) {
					filteredMessages.add(message);
				}
				else {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Fetched email with subject '" + message.getSubject()
								+ "' will be discarded by the matching filter and will not be flagged as SEEN.");
					}
				}
			}
			else {
				filteredMessages.add(message);
			}
		}
		return filteredMessages.toArray(new MimeMessage[0]);
	}

	/**
	 * Fetches the specified messages from this receiver's folder. Default
	 * implementation {@link Folder#fetch(Message[], FetchProfile) fetches}
	 * every {@link javax.mail.FetchProfile.Item}.
	 * @param messages the messages to fetch
	 * @throws MessagingException in case of JavaMail errors
	 */
	protected void fetchMessages(Message[] messages) throws MessagingException {
		FetchProfile contentsProfile = new FetchProfile();
		contentsProfile.add(FetchProfile.Item.ENVELOPE);
		contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
		contentsProfile.add(FetchProfile.Item.FLAGS);
		this.folder.fetch(messages, contentsProfile);
	}

	/**
	 * Deletes the given messages from this receiver's folder.
	 * @param messages the messages to delete
	 * @throws MessagingException in case of JavaMail errors
	 */
	protected void deleteMessages(Message[] messages) throws MessagingException {
		for (Message message : messages) {
			message.setFlag(Flags.Flag.DELETED, true);
		}
	}

	/**
	 * Optional method allowing you to set additional flags.
	 * Currently only implemented in IMapMailReceiver.
	 * @param message The message.
	 * @throws MessagingException A MessagingException.
	 */
	protected void setAdditionalFlags(Message message) throws MessagingException {
	}

	@Override
	public void destroy() {
		this.folderWriteLock.lock();
		try {
			closeFolder();
			MailTransportUtils.closeService(this.store);
			this.folder = null;
			this.store = null;
		}
		finally {
			this.folderWriteLock.unlock();
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.folderOpenMode = Folder.READ_WRITE;
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	public String toString() {
		return this.url.toString();
	}

	Store getStore() {
		return this.store;
	}

	/**
	 * Since we copy the message to eagerly fetch the message, it has no folder.
	 * However, we need to make a folder available in case the user wants to
	 * perform operations on the message in the folder later in the flow.
	 *
	 * @author Gary Russell
	 *
	 * @since 2.2
	 */
	private final class IntegrationMimeMessage extends MimeMessage {

		private final MimeMessage source;

		private final Object content;

		IntegrationMimeMessage(MimeMessage source) throws MessagingException {
			super(source);
			this.source = source;
			if (AbstractMailReceiver.this.simpleContent) {
				this.content = null;
			}
			else {
				Object complexContent;
				try {
					complexContent = source.getContent();
				}
				catch (IOException e) {
					complexContent = "Unable to extract content; see logs: " + e.getMessage();
					AbstractMailReceiver.this.logger.error("Failed to extract content from " + source, e);
				}
				this.content = complexContent;
			}
		}

		@Override
		public Folder getFolder() {
			if (!AbstractMailReceiver.this.autoCloseFolder) {
				return AbstractMailReceiver.this.folder;
			}
			else {
				try {
					return obtainFolderInstance();
				}
				catch (MessagingException e) {
					throw new org.springframework.messaging.MessagingException("Unable to obtain the mail folder", e);
				}
			}
		}

		@Override
		public Date getReceivedDate() throws MessagingException {
			/*
			 * Basic MimeMessage always returns null; delegate to the original.
			 */
			return this.source.getReceivedDate();
		}

		@Override
		public int getLineCount() throws MessagingException {
			/*
			 * Basic MimeMessage always returns '-1'; delegate to the original.
			 */
			return this.source.getLineCount();
		}

		@Override
		public Object getContent() throws IOException, MessagingException {
			if (AbstractMailReceiver.this.simpleContent) {
				return super.getContent();
			}
			else {
				return this.content;
			}
		}

	}

}
