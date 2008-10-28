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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.mail.monitor.AsyncMonitoringStrategy;
import org.springframework.integration.mail.monitor.MailTransportUtils;
import org.springframework.integration.mail.monitor.MonitoringStrategy;
import org.springframework.util.Assert;

/**
 * A Connection to a mail folder capable of retrieving mail by utilizing the
 * given instance of {@link MonitoringStrategy}.
 * 
 * @author Jonas Partner
 */
public class DefaultFolderConnection implements Lifecycle, DisposableBean, FolderConnection {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final URLName storeUri;

	private final MonitoringStrategy monitoringStrategy;

	private final boolean polling;

	private volatile Session session;

	private volatile Store store;

	private volatile Folder folder;

	private volatile Properties javaMailProperties = new Properties();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public DefaultFolderConnection(String storeUri, MonitoringStrategy monitoringStrategy, boolean polling) {
		Assert.notNull(storeUri, "storeUri must not be null");
		Assert.notNull(monitoringStrategy, "monitoringStrategy must not ne null");
		this.storeUri = new URLName(storeUri);
		this.monitoringStrategy = monitoringStrategy;
		this.polling = polling;
		Assert.isTrue(polling || AsyncMonitoringStrategy.class.isAssignableFrom(monitoringStrategy.getClass()),
				"Folder connection requires an AsyncMonitoringStrategy if polling is disabled.");
	}


	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
	}

	public synchronized Message[] receive() {
		try {
			if (!this.isRunning()) {
				this.start();
			}
			if (!this.polling) {
				((AsyncMonitoringStrategy) this.monitoringStrategy).waitForNewMessages(this.folder);
			}
			return this.monitoringStrategy.receive(this.folder);
		}
		catch (Exception e) {
			throw new org.springframework.integration.core.MessagingException(
					"failure occurred while receiving from folder", e);
		}
	}

	@Override
	public String toString() {
		return this.storeUri.toString();
	}

	public void destroy() throws Exception {
		this.stop();
	}

	/*
	 * Lifecycle implementation
	 */

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public synchronized void start() {
		synchronized (this.lifecycleMonitor) {
			try {
				this.openSession();
				this.openFolder();
				this.running = true;
			}
			catch (MessagingException e) {
				throw new org.springframework.integration.core.MessagingException(
						"Failed to start FolderConnection", e);
			}
		}
	}

	public synchronized void stop() {
		synchronized (this.lifecycleMonitor) {
			MailTransportUtils.closeFolder(this.folder);
			MailTransportUtils.closeService(this.store);
			this.folder = null;
			this.store = null;
			this.running = false;
		}
	}

	private void openFolder() throws MessagingException {
		this.folder = this.store.getFolder(this.storeUri);
		if (this.folder == null || !this.folder.exists()) {
			throw new IllegalStateException("no default folder available");
		}
		if (this.folder.isOpen()) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Opening folder [" + MailTransportUtils.toPasswordProtectedString(this.storeUri) + "]");
		}
		this.folder.open(this.monitoringStrategy.getFolderOpenMode());
	}

	private void openSession() throws MessagingException {
		this.session = Session.getInstance(this.javaMailProperties);
		this.store = this.session.getStore(this.storeUri);
		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to store [" + MailTransportUtils.toPasswordProtectedString(this.storeUri) + "]");
		}
		this.store.connect();
	}

}
