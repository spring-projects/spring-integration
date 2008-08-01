/*
 * Copyright 2002-2007 the original author or authors.
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
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.mail.monitor.AsyncMonitoringStrategy;
import org.springframework.integration.adapter.mail.monitor.MailTransportUtils;
import org.springframework.integration.adapter.mail.monitor.MonitoringStrategy;
import org.springframework.util.Assert;

/**
 * @author Jonas Partner
 *
 */
public class DefaultFolderConnection implements Lifecycle, InitializingBean,
		DisposableBean, FolderConnection {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final URLName storeUri;

	private final Session session;

	private final MonitoringStrategy monitoringStrategy;

	private final boolean polling;

	private Store store;

	private Folder folder;

	public DefaultFolderConnection(String storeUri, Properties javaMailProperties,
			MonitoringStrategy monitoringStrategy, boolean polling) {
		this.storeUri = new URLName(storeUri);
		this.session = Session.getInstance(javaMailProperties);
		this.monitoringStrategy = monitoringStrategy;
		this.polling = polling;
		if (!polling
				&& monitoringStrategy.getClass().isAssignableFrom(AsyncMonitoringStrategy.class)) {
			throw new ConfigurationException(
					"Folder connection requires an AsyncMonitoringStragey if polling is disabled");
		}
	}

	public void afterPropertiesSet() throws Exception {

		Assert.notNull(storeUri, "Property 'storeUri' is required");
		Assert.notNull(session, "Property 'JavaMailProperties' is required");
		Assert.notNull(monitoringStrategy,
				"An instantce of MonitoringStrategy' is required");
		start();
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.adapter.mail.FolderConnectionI#receive()
	 */
	public synchronized Message[] receive() {
		if (!isRunning()) {
			throw new org.springframework.integration.message.MessagingException(
					"Folder connection is not running");
		}
	
		try {
			if (!polling) {
				((AsyncMonitoringStrategy) monitoringStrategy)
						.waitForNewMessages(folder);
			}
			return monitoringStrategy.receive(folder);
		} catch (Exception e) {
			throw new org.springframework.integration.message.MessagingException(
					"Exception receiving from folder", e);
		}

	}

	public void destroy() throws Exception {
		stop();
	}

	public synchronized boolean isRunning() {
		return (folder!= null && folder.isOpen());
	}

	public synchronized void start() {
		try {
			openSession();
			openFolder();
		} catch (MessagingException messageE) {
			throw new org.springframework.integration.message.MessagingException(
					"Excpetion starting MailSource", messageE);
		}

	}

	public synchronized void stop() {
		MailTransportUtils.closeFolder(folder);
		MailTransportUtils.closeService(store);
		folder = null;
		store = null;
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
			logger.debug("Opening folder ["
					+ MailTransportUtils.toPasswordProtectedString(storeUri)
					+ "]");
		}
		folder.open(monitoringStrategy.getFolderOpenMode());
	}

	private void openSession() throws MessagingException {
		store = session.getStore(storeUri);
		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to store ["
					+ MailTransportUtils.toPasswordProtectedString(storeUri)
					+ "]");
		}
		store.connect();
	}

}
