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

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.adapter.mail.monitor.AsyncMonitoringStrategy;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.SubscribableSource;

/**
 * Broadcasts all mail messages recovered to subscribed {@link MessageTarget}
 * The given {@link FolderConnection} should be using an
 * {@link AsyncMonitoringStrategy} to retrieve mail
 * 
 * @author Jonas Partner
 * 
 */
public class SubscribableMailSource implements SubscribableSource, Lifecycle,
		DisposableBean {

	private final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();

	private final TaskExecutor taskExecutor;

	private final MonitorRunnable monitorRunnable;

	private boolean monitorRunning = false;

	private final Log logger = LogFactory.getLog(getClass());

	private MailMessageConverter converter = new DefaultMailMessageConverter();

	public SubscribableMailSource(FolderConnection folderConnection,
			TaskExecutor taskExecutor) {
		this.monitorRunnable = new MonitorRunnable(folderConnection);
		this.taskExecutor = taskExecutor;

	}

	public void setApplySequence(boolean applySequence) {
		this.dispatcher.setApplySequence(applySequence);
	}

	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.addTarget(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.removeTarget(target);
	}

	public void setConverter(MailMessageConverter converter) {
		this.converter = converter;
	}

	public void destroy() throws Exception {
		stop();
	}

	public void start() {
		logger.info("Starting to monitor mailbox");
		startMonitor();
		logger.info("Started to monitor mailbox");

	}

	public void stop() {
		logger.info("Stopping monitoring of mailbox");
		stopMonitor();
		logger.info("Stopped monitoring mailbox");
	}

	public boolean isRunning() {
		return monitorRunning;
	}

	protected synchronized void startMonitor() {
		if (!monitorRunning) {
			taskExecutor.execute(monitorRunnable);
		}
	}

	protected synchronized void stopMonitor() {
		if (monitorRunning) {
			monitorRunnable.interrupt();
		}
	}

	private class MonitorRunnable implements Runnable {
		private volatile Thread thread;

		private final FolderConnection folderConnection;

		protected MonitorRunnable(FolderConnection folderConnection) {
			this.folderConnection = folderConnection;
		}

		public synchronized void interrupt() {
			thread.interrupt();
		}

		public void run() {
			thread = Thread.currentThread();
			while (!Thread.currentThread().isInterrupted()) {
				Message[] messages = folderConnection.receive();
				for (Message message : messages) {
					dispatcher.send(converter.create((MimeMessage) message));
				}
			}

		}
	}

}
