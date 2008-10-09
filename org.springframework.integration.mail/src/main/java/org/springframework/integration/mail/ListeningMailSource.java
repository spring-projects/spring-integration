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

import java.util.Date;

import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageProducingEndpoint;
import org.springframework.integration.mail.monitor.AsyncMonitoringStrategy;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * An event-driven mail source that sends Spring Integration Messages to its
 * output channel. The Message payload will be the {@link javax.mail.Message}
 * instance that was received. The given {@link FolderConnection} should be
 * using an {@link AsyncMonitoringStrategy} to retrieve mail.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class ListeningMailSource extends AbstractMessageProducingEndpoint implements Lifecycle, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MonitorRunnable monitorRunnable;

	private volatile boolean monitorRunning = false;


	public ListeningMailSource(FolderConnection folderConnection) {
		Assert.notNull(folderConnection, "FolderConnection must not be null");
		this.monitorRunnable = new MonitorRunnable(folderConnection);
	}


	// Lifecycle implementation

	public boolean isRunning() {
		return this.monitorRunning;
	}

	public void start() {
		this.startMonitor();
		if (logger.isInfoEnabled()) {
			logger.info("started monitoring mailbox");
		}
	}

	public void stop() {
		this.stopMonitor();
		if (logger.isInfoEnabled()) {
			logger.info("stopped monitoring mailbox");
		}
	}

	protected void startMonitor() {
		synchronized (this.monitorRunnable) {
			if (!this.monitorRunning) {
				Assert.state(this.getTaskScheduler() != null, "TaskScheduler is required");
				this.getTaskScheduler().schedule(this.monitorRunnable,
						new Trigger() {
							public Date getNextRunTime(Date lastScheduledRunTime, Date lastCompleteTime) {
								return new Date();
							}
						}
				);
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

	public void destroy() throws Exception {
		this.stop();
	}


	private class MonitorRunnable implements Runnable {

		private volatile Thread thread;

		private final FolderConnection folderConnection;


		private MonitorRunnable(FolderConnection folderConnection) {
			this.folderConnection = folderConnection;
		}


		public synchronized void interrupt() {
			if (this.thread != null) {
				this.thread.interrupt();
			}
			else if (logger.isInfoEnabled()) {
				logger.info("monitor is not running, cannot interrupt");
			}
		}

		public void run() {
			this.thread = Thread.currentThread();
			while (!Thread.currentThread().isInterrupted()) {
				if (!this.folderConnection.isRunning()) {
					this.folderConnection.start();
				}
				Message[] mailMessages = this.folderConnection.receive();
				for (Message mailMessage : mailMessages) {
					ListeningMailSource.this.sendMessage(MessageBuilder.withPayload(mailMessage).build());
				}
			}
		}
	}

}
