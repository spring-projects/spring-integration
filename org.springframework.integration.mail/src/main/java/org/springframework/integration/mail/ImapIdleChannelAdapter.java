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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.context.Lifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.endpoint.AbstractMessageProducingEndpoint;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

/**
 * An event-driven Channel Adapter that receives mail messages from a mail
 * server that supports the IMAP "idle" command (see RFC 2177). Received mail
 * messages will be converted and sent as Spring Integration Messages to the
 * output channel. The Message payload will be the {@link javax.mail.Message}
 * instance that was received.
 * 
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public class ImapIdleChannelAdapter extends AbstractMessageProducingEndpoint implements Lifecycle {

	private final IdleTask idleTask = new IdleTask();

	private volatile TaskExecutor taskExecutor;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	private final ImapMailReceiver mailReceiver;


	public ImapIdleChannelAdapter(ImapMailReceiver mailReceiver) {
		Assert.notNull(mailReceiver, "mailReceiver must not be null");
		this.mailReceiver = mailReceiver;
	}

	public ImapIdleChannelAdapter(String url) {
		Assert.isTrue(url.startsWith("imap"), "url must start with 'imap'");
		this.mailReceiver = new ImapMailReceiver(url);
	}


	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setJavaMailProperties(Properties javaMailProperties) {
		this.mailReceiver.setJavaMailProperties(javaMailProperties);
	}

	public void setShouldDeleteMessages(boolean shouldDeleteMessages) {
		this.mailReceiver.setShouldDeleteMessages(shouldDeleteMessages);
	}

	protected void handleMailMessagingException(MessagingException e) {
		if (logger.isWarnEnabled()) {
			logger.warn("error occurred in idle task", e);
		}
	}

	/*
	 * Lifecycle implementation
	 */

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				if (this.taskExecutor == null) {
					if (logger.isInfoEnabled()) {
						logger.info("No TaskExecutor has been provided, will use a ["
								+ SimpleAsyncTaskExecutor.class + "] as the default.");
					}
					this.taskExecutor = new SimpleAsyncTaskExecutor();
				}
				this.taskExecutor.execute(this.idleTask);
			}
			this.running = true;
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				this.idleTask.interrupt();
			}
			this.running = false;
		}
	}


	private class IdleTask implements Runnable {

		private volatile Thread thread;

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
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("waiting for mail");
					}
					mailReceiver.waitForNewMessages();
					Message[] mailMessages = mailReceiver.receive();
					if (logger.isDebugEnabled()) {
						logger.debug("received " + mailMessages.length + " mail messages");
					}
					for (Message mailMessage : mailMessages) {
						MimeMessage copied = new MimeMessage((MimeMessage) mailMessage);
						sendMessage(MessageBuilder.withPayload(copied).build());
					}
				}
				catch (MessagingException e) {
					handleMailMessagingException(e);
					return;
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

}
