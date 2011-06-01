/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
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
 * @author Oleg Zhurakousky
 */
public class ImapIdleChannelAdapter extends MessageProducerSupport {

	private final IdleTask idleTask = new IdleTask();

	private volatile boolean shouldReconnectAutomatically = true;

	private volatile Executor taskExecutor;

	private final ImapMailReceiver mailReceiver;
	
	private volatile int reconnectDelay = 10000; // seconds
	
	private volatile ScheduledFuture<?> scheduledFuture;
	
	private volatile ResubmittingTask task;
	
	private volatile long connectionPingInterval = 10000;

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
	
	public String getComponentType(){
		return "mail:imap-idle-channel-adapter";
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
		TaskScheduler scheduler =  this.getTaskScheduler();
		Assert.notNull(scheduler, "'taskScheduler' must not be null" );
		this.task = new ResubmittingTask(this.idleTask, scheduler, reconnectDelay);
		this.task.start();
		task.setTaskExecutor(taskExecutor);
		scheduledFuture = scheduler.schedule(task, new Date());
		scheduler = this.getTaskScheduler();
		if (scheduler != null) {
			scheduler.scheduleAtFixedRate(new Runnable() {	
				public void run() {
					try {
						Store store = mailReceiver.getStore();
						if (store != null) {
							store.isConnected();
						}		
					} 
					catch (Exception ignore) {
					}
				}
			}, connectionPingInterval);
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		scheduledFuture.cancel(true);
		this.task.stop();
		try {
			mailReceiver.destroy();
		} catch (Exception e) {
			throw new IllegalStateException("Failure during the destruction of " + mailReceiver, e);
		}
		
	}

	private class IdleTask implements Runnable {

		public void run() {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("waiting for mail");
				}
				mailReceiver.waitForNewMessages();
				if (task.isRunning()){
					Message[] mailMessages = mailReceiver.receive();
					if (logger.isDebugEnabled()) {
						logger.debug("received " + mailMessages.length + " mail messages");
					}
					for (Message mailMessage : mailMessages) {
						MimeMessage copied = new MimeMessage((MimeMessage) mailMessage);
						sendMessage(MessageBuilder.withPayload(copied).build());
					}
				}			
			} catch (MessagingException e) {
				ImapIdleChannelAdapter.this.handleMailMessagingException(e);
				if (shouldReconnectAutomatically){
					throw new IllegalStateException("Failure in 'idle' task. Will resubmit", e);
				}
				else {
					throw new org.springframework.integration.MessagingException("Failure in 'idle' task. Will NOT resubmit", e);
				}
			}
		}
	}
}
