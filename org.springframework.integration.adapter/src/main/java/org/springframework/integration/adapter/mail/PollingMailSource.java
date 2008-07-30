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

package org.springframework.integration.adapter.mail;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.adapter.mail.monitor.DefaultLocalMailMessageStore;
import org.springframework.integration.adapter.mail.monitor.LocalMailMessageStore;
import org.springframework.integration.adapter.mail.monitor.MailTransportUtils;
import org.springframework.integration.adapter.mail.monitor.MonitoringStrategy;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollableSource;
import org.springframework.util.Assert;

/**
 * {@link MessageSource} implementation which delegates to a
 * {@link MonitoringStrategy} to poll a mailbox. Each poll of the mailbox may
 * return more than one message which will then be stored locally using the
 * provided {@link LocalMailMessageStore}
 * 
 * @author Jonas Partner
 */
@SuppressWarnings("unchecked")
public class PollingMailSource implements PollableSource, DisposableBean, Lifecycle {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MonitoringStrategy monitoringStrategy;

	private Session session;

	private Store store;

	private Folder folder;

	private URLName storeUri;

	private MailMessageConverter converter = new DefaultMailMessageConverter();

	private LocalMailMessageStore mailMessageStore = new DefaultLocalMailMessageStore();

	public PollingMailSource(MonitoringStrategy monitoringStrategy) {
		this.monitoringStrategy = monitoringStrategy;
	}

	@SuppressWarnings("unchecked")
	public Message receive() {
		Message received = null;
		javax.mail.Message mailMessage = mailMessageStore.getNext();
		if (mailMessage == null) {
			try {
				javax.mail.Message[] messages = monitoringStrategy.receive(folder);
				mailMessageStore.addLast(messages);
				mailMessage = mailMessageStore.getNext();
			}
			catch (Exception e) {
				throw new org.springframework.integration.message.MessagingException("Excpetion receiving mail", e);
			}
		}
		if (mailMessage != null) {
			received = converter.create((MimeMessage) mailMessage);
			if (logger.isDebugEnabled()) {
				logger.debug("Received message " + received);
			}
		}
		return received;
	}

	public void setJavaMailProperties(Properties javaMailProperties) {
		session = Session.getInstance(javaMailProperties, null);
	}
	
	public void setJavaMailsession(Session session) {
		this.session = session;
	}

	public void setStoreUri(String storeUri) {
		this.storeUri = new URLName(storeUri);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(storeUri, "Property 'storeUri' is required");
		Assert.notNull(session, "Property 'JavaMailProperties' is required");
		Assert.notNull(converter, "An instantce of MailMessageConverter' is required");
		openSession();
		openFolder();
	}

	private void openFolder() throws MessagingException {
		if (folder != null && folder.isOpen()) {
			return;
		}
		folder = store.getFolder(storeUri);
		if (folder == null || !folder.exists()) {
			throw new IllegalStateException("No default folder to receive from");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Opening folder [" + MailTransportUtils.toPasswordProtectedString(storeUri) + "]");
		}
		folder.open(monitoringStrategy.getFolderOpenMode());
	}

	private void openSession() throws MessagingException {
		store = session.getStore(storeUri);
		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to store [" + MailTransportUtils.toPasswordProtectedString(storeUri) + "]");
		}
		store.connect();
	}

	public void setConverter(MailMessageConverter converter) {
		this.converter = converter;
	}

	public void destroy() throws Exception {
		stop();
	}

	public boolean isRunning() {
		return folder.isOpen();
	}

	public void start() {
		try {
			openSession();
			openFolder();
		}
		catch (MessagingException messageE) {
			throw new org.springframework.integration.message.MessagingException("Excpetion starting MailSource",
					messageE);
		}

	}

	public void stop() {
		MailTransportUtils.closeFolder(folder);
		MailTransportUtils.closeService(store);
	}

	public void setMailMessageStore(LocalMailMessageStore mailMessageStore) {
		this.mailMessageStore = mailMessageStore;
	}

}
