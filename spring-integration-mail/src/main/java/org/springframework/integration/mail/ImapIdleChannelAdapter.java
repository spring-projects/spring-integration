/*
 * Copyright 2002-2010 the original author or authors.
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

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.mail.internet.MimeMessage;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
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
public class ImapIdleChannelAdapter extends MessageProducerSupport {

	private final IdleTask idleTask = new IdleTask();

	private volatile boolean shouldReconnectAutomatically = true;

	private volatile Executor taskExecutor;

	private final ImapMailReceiver mailReceiver;
	
	private volatile boolean reconnecting;
	
	private volatile int reconnectDelay = 10; // seconds


	public ImapIdleChannelAdapter(ImapMailReceiver mailReceiver) {
		Assert.notNull(mailReceiver, "mailReceiver must not be null");
		this.mailReceiver = mailReceiver;
	}


	/**
	 * Specify whether the IDLE task should reconnect automatically after
	 * catching a {@link FolderClosedException} while waiting for messages.
	 * The default value is <code>true</code>.
	 */
	public void setShouldReconnectAutomatically(boolean shouldReconnectAutomatically) {
		this.shouldReconnectAutomatically = shouldReconnectAutomatically;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	protected void handleMailMessagingException(MessagingException e) {
		if (logger.isWarnEnabled()) {
			logger.warn("error occurred in idle task", e);
		}
	}

	/*
	 * Lifecycle implementation
	 */

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		if (this.taskExecutor == null) {
			if (logger.isInfoEnabled()) {
				logger.info("No TaskExecutor has been provided, will use a ["
						+ SimpleAsyncTaskExecutor.class + "] as the default.");
			}
			this.taskExecutor = new SimpleAsyncTaskExecutor();
		}
		this.taskExecutor.execute(this.idleTask);
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		this.idleTask.interrupt();
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
					reconnecting = false;
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
					if (shouldReconnectAutomatically){
						if (e instanceof FolderClosedException || 
							e instanceof StoreClosedException || 
							(e.getNextException() instanceof UnknownHostException && reconnecting)){
							waitToReconnect();
							continue;
						}
					}
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
	public String getComponentType(){
		return "mail:imap-idle-channel-adapter";
	}

	private void waitToReconnect() {
		CountDownLatch latch = new CountDownLatch(1);
		try {
			logger.warn("Waiting " + reconnectDelay + " seconds before attemptong to reconnect to host");
			latch.await(this.reconnectDelay, TimeUnit.SECONDS);
			reconnecting = true;
			logger.warn("Will attempt to reconnect to host now");
		} catch (Exception ignore) {
		}
	}
}
