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

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.adapter.mail.monitor.AsyncMonitoringStrategy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;

/**
 * Broadcasts all mail messages recovered to subscribed {@link MessageTarget MessageTargets}.
 * The given {@link FolderConnection} should be using an {@link AsyncMonitoringStrategy} to
 * retrieve mail.
 * 
 * @author Jonas Partner
 */
public class SubscribableMailSource implements MessageSource, Lifecycle, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel outputChannel;

	private final TaskExecutor taskExecutor;

	private final MonitorRunnable monitorRunnable;

	private volatile boolean monitorRunning = false;

	private volatile MailMessageConverter converter = new DefaultMailMessageConverter();


	public SubscribableMailSource(FolderConnection folderConnection, TaskExecutor taskExecutor) {
		Assert.notNull(folderConnection, "FolderConnection must not be null");
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.monitorRunnable = new MonitorRunnable(folderConnection);
		this.taskExecutor = taskExecutor;
	}


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setConverter(MailMessageConverter converter) {
		this.converter = converter;
	}

	public void destroy() throws Exception {
		this.stop();
	}

	public void start() {
		if (logger.isInfoEnabled()) {
			logger.info("Starting to monitor mailbox");
		}
		this.startMonitor();
		if (logger.isInfoEnabled()) {
			logger.info("Started to monitor mailbox");
		}

	}

	public void stop() {
		if (logger.isInfoEnabled()) {
			logger.info("Stopping monitoring of mailbox");
		}
		this.stopMonitor();
		if (logger.isInfoEnabled()) {
			logger.info("Stopped monitoring mailbox");
		}
	}

	public boolean isRunning() {
		return this.monitorRunning;
	}

	protected void startMonitor() {
		synchronized (this.monitorRunnable) {
			if (!this.monitorRunning) {
				this.taskExecutor.execute(this.monitorRunnable);
			}
			this.monitorRunning = true;
		}
	}

	protected void stopMonitor() {
		synchronized (this.monitorRunnable) {
			if (this.monitorRunning) {
				this.monitorRunnable.interrupt();
			}
			this.monitorRunning = false;
		}
	}


	private class MonitorRunnable implements Runnable {

		private volatile Thread thread;

		private final FolderConnection folderConnection;


		private MonitorRunnable(FolderConnection folderConnection) {
			this.folderConnection = folderConnection;
		}


		public synchronized void interrupt() {
			this.thread.interrupt();
		}

		public void run() {
			this.thread = Thread.currentThread();
			while (!Thread.currentThread().isInterrupted()) {
				Message[] messages = this.folderConnection.receive();
				for (Message message : messages) {
					outputChannel.send(converter.create((MimeMessage) message));
				}
			}
		}
	}

}
