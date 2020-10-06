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

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mail.event.MailIntegrationEvent;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.IntegrationResourceHolderSynchronization;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ImapIdleChannelAdapter extends MessageProducerSupport implements BeanClassLoaderAware,
		ApplicationEventPublisherAware {

	private static final int DEFAULT_RECONNECT_DELAY = 10000;

	private final ExceptionAwarePeriodicTrigger receivingTaskTrigger = new ExceptionAwarePeriodicTrigger();

	private final IdleTask idleTask = new IdleTask();

	private final ImapMailReceiver mailReceiver;

	private TransactionSynchronizationFactory transactionSynchronizationFactory;

	private ClassLoader classLoader;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean shouldReconnectAutomatically = true;

	private Executor sendingTaskExecutor = Executors.newFixedThreadPool(1);

	private boolean sendingTaskExecutorSet;

	private List<Advice> adviceChain;

	private long reconnectDelay = DEFAULT_RECONNECT_DELAY; // milliseconds

	private volatile ScheduledFuture<?> receivingTask;

	public ImapIdleChannelAdapter(ImapMailReceiver mailReceiver) {
		Assert.notNull(mailReceiver, "'mailReceiver' must not be null");
		this.mailReceiver = mailReceiver;
	}

	public void setTransactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {

		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	/**
	 * Specify an {@link Executor} used to send messages received by the
	 * adapter.
	 * @param sendingTaskExecutor the sendingTaskExecutor to set
	 */
	public void setSendingTaskExecutor(Executor sendingTaskExecutor) {
		Assert.notNull(sendingTaskExecutor, "'sendingTaskExecutor' must not be null");
		this.sendingTaskExecutor = sendingTaskExecutor;
		this.sendingTaskExecutorSet = true;
	}

	/**
	 * Specify whether the IDLE task should reconnect automatically after
	 * catching a {@link javax.mail.FolderClosedException} while waiting for messages. The
	 * default value is <code>true</code>.
	 * @param shouldReconnectAutomatically true to reconnect.
	 */
	public void setShouldReconnectAutomatically(boolean shouldReconnectAutomatically) {
		this.shouldReconnectAutomatically = shouldReconnectAutomatically;
	}

	/**
	 * The time between connection attempts in milliseconds (default 10 seconds).
	 * @param reconnectDelay the reconnectDelay to set
	 * @since 3.0.5
	 */
	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	@Override
	public String getComponentType() {
		return "mail:imap-idle-channel-adapter";
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/*
	 * Lifecycle implementation
	 */

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		TaskScheduler scheduler = getTaskScheduler();
		Assert.notNull(scheduler, "'taskScheduler' must not be null");
		this.receivingTask = scheduler.schedule(new ReceivingTask(), this.receivingTaskTrigger);
	}

	@Override
	// guarded by super#lifecycleLock
	protected void doStop() {
		this.receivingTask.cancel(true);
		this.mailReceiver.cancelPing();
	}

	@Override
	public void destroy() {
		super.destroy();
		this.mailReceiver.destroy();
		// If we're running with the default executor, shut it down.
		if (!this.sendingTaskExecutorSet && this.sendingTaskExecutor != null) {
			((ExecutorService) this.sendingTaskExecutor).shutdown();
		}
	}

	private Runnable createMessageSendingTask(Object mailMessage) {
		Runnable sendingTask = prepareSendingTask(mailMessage);

		// wrap in the TX proxy if necessary
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(sendingTask);
			if (!CollectionUtils.isEmpty(this.adviceChain)) {
				for (Advice advice : this.adviceChain) {
					proxyFactory.addAdvice(advice);
				}
			}
			sendingTask = (Runnable) proxyFactory.getProxy(this.classLoader);
		}
		return sendingTask;
	}

	private Runnable prepareSendingTask(Object mailMessage) {
		return () -> {
			@SuppressWarnings("unchecked")
			org.springframework.messaging.Message<?> message =
					mailMessage instanceof Message
							? getMessageBuilderFactory().withPayload(mailMessage).build()
							: (org.springframework.messaging.Message<Object>) mailMessage;

			if (TransactionSynchronizationManager.isActualTransactionActive()
					&& this.transactionSynchronizationFactory != null) {

				TransactionSynchronization synchronization = this.transactionSynchronizationFactory.create(this);
				if (synchronization != null) {
					TransactionSynchronizationManager.registerSynchronization(synchronization);

					if (synchronization instanceof IntegrationResourceHolderSynchronization
							&& !TransactionSynchronizationManager.hasResource(this)) {

						TransactionSynchronizationManager.bindResource(this,
								((IntegrationResourceHolderSynchronization) synchronization).getResourceHolder());
					}

					Object resourceHolder = TransactionSynchronizationManager.getResource(this);
					if (resourceHolder instanceof IntegrationResourceHolder) {
						((IntegrationResourceHolder) resourceHolder).setMessage(message);
					}
				}
			}
			sendMessage(message);
		};
	}

	private void publishException(Exception e) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new ImapIdleExceptionEvent(e));
		}
		else {
			logger.debug(() -> "No application event publisher for exception: " + e.getMessage());
		}
	}


	private class ReceivingTask implements Runnable {

		ReceivingTask() {
		}

		@Override
		public void run() {
			if (isRunning()) {
				try {
					ImapIdleChannelAdapter.this.idleTask.run();
					logger.debug("Task completed successfully. Re-scheduling it again right away.");
				}
				catch (Exception ex) { //run again after a delay
					logger.warn(ex, () -> "Failed to execute IDLE task. Will attempt to resubmit in "
							+ ImapIdleChannelAdapter.this.reconnectDelay + " milliseconds.");
					ImapIdleChannelAdapter.this.receivingTaskTrigger.delayNextExecution();
					publishException(ex);
				}
			}
		}

	}


	private class IdleTask implements Runnable {

		IdleTask() {
		}

		@Override
		public void run() {
			if (isRunning()) {
				try {
					logger.debug("waiting for mail");
					ImapIdleChannelAdapter.this.mailReceiver.waitForNewMessages();
					Folder folder = ImapIdleChannelAdapter.this.mailReceiver.getFolder();
					if (folder != null && folder.isOpen() && isRunning()) {
						Object[] mailMessages = ImapIdleChannelAdapter.this.mailReceiver.receive();
						logger.debug(() -> "received " + mailMessages.length + " mail messages");
						for (Object mailMessage : mailMessages) {
							Runnable messageSendingTask = createMessageSendingTask(mailMessage);
							if (isRunning()) {
								ImapIdleChannelAdapter.this.sendingTaskExecutor.execute(messageSendingTask);
							}
						}
					}
				}
				catch (MessagingException ex) {
					logger.warn(ex, "error occurred in idle task");
					if (ImapIdleChannelAdapter.this.shouldReconnectAutomatically) {
						throw new IllegalStateException("Failure in 'idle' task. Will resubmit.", ex);
					}
					else {
						throw new org.springframework.messaging.MessagingException(
								"Failure in 'idle' task. Will NOT resubmit.", ex);
					}
				}
			}
		}

	}

	private class ExceptionAwarePeriodicTrigger implements Trigger {

		private volatile boolean delayNextExecution;


		ExceptionAwarePeriodicTrigger() {
		}

		@Override
		public Date nextExecutionTime(TriggerContext triggerContext) {
			if (this.delayNextExecution) {
				this.delayNextExecution = false;
				return new Date(System.currentTimeMillis() + ImapIdleChannelAdapter.this.reconnectDelay);
			}
			else {
				return new Date(System.currentTimeMillis());
			}
		}

		void delayNextExecution() {
			this.delayNextExecution = true;
		}

	}

	public class ImapIdleExceptionEvent extends MailIntegrationEvent {

		private static final long serialVersionUID = -5875388810251967741L;

		ImapIdleExceptionEvent(Exception e) {
			super(ImapIdleChannelAdapter.this, e);
		}

	}

}
