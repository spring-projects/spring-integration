/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

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

	private final IdleTask idleTask = new IdleTask();

	private volatile Executor sendingTaskExecutor;

	private volatile boolean sendingTaskExecutorSet;

	private volatile boolean shouldReconnectAutomatically = true;

	private volatile ClassLoader classLoader;

	private volatile List<Advice> adviceChain;

	private final ImapMailReceiver mailReceiver;

	private volatile int reconnectDelay = 10000; // milliseconds

	private volatile ScheduledFuture<?> receivingTask;

	private volatile ScheduledFuture<?> pingTask;

	private volatile long connectionPingInterval = 10000;

	private final ExceptionAwarePeriodicTrigger receivingTaskTrigger = new ExceptionAwarePeriodicTrigger();

	private volatile TransactionSynchronizationFactory transactionSynchronizationFactory;

	private volatile ApplicationEventPublisher applicationEventPublisher;

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
	 * catching a {@link FolderClosedException} while waiting for messages. The
	 * default value is <code>true</code>.
	 * @param shouldReconnectAutomatically true to reconnect.
	 */
	public void setShouldReconnectAutomatically(boolean shouldReconnectAutomatically) {
		this.shouldReconnectAutomatically = shouldReconnectAutomatically;
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
		final TaskScheduler scheduler =  this.getTaskScheduler();
		Assert.notNull(scheduler, "'taskScheduler' must not be null" );
		if (this.sendingTaskExecutor == null) {
			this.sendingTaskExecutor = Executors.newFixedThreadPool(1);
		}
		this.receivingTask = scheduler.schedule(new ReceivingTask(), this.receivingTaskTrigger);
		this.pingTask = scheduler.scheduleAtFixedRate(new PingTask(), this.connectionPingInterval);
	}

	@Override
	// guarded by super#lifecycleLock
	protected void doStop() {
		this.receivingTask.cancel(true);
		this.pingTask.cancel(true);
		try {
			this.mailReceiver.destroy();
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Failure during the destruction of Mail receiver: " + mailReceiver, e);
		}
		/*
		 * If we're running with the default executor, shut it down.
		 */
		if (!this.sendingTaskExecutorSet && this.sendingTaskExecutor != null) {
			((ExecutorService) sendingTaskExecutor).shutdown();
			this.sendingTaskExecutor = null;
		}
	}


	private class ReceivingTask implements Runnable {
		@Override
		public void run() {
			try {
				idleTask.run();
				if (logger.isDebugEnabled()) {
					logger.debug("Task completed successfully. Re-scheduling it again right away.");
				}
			}
			catch (Exception e) { //run again after a delay
				logger.warn("Failed to execute IDLE task. Will attempt to resubmit in " + reconnectDelay + " milliseconds.", e);
				receivingTaskTrigger.delayNextExecution();
				ImapIdleChannelAdapter.this.publishException(e);
			}
		}
	}


	private class IdleTask implements Runnable {

		@Override
		public void run() {
			final TaskScheduler scheduler =  getTaskScheduler();
			Assert.notNull(scheduler, "'taskScheduler' must not be null" );
			/*
			 * The following shouldn't be necessary because doStart() will have ensured we have
			 * one. But, just in case...
			 */
			Assert.state(sendingTaskExecutor != null, "'sendingTaskExecutor' must not be null");

			try {
				if (logger.isDebugEnabled()) {
					logger.debug("waiting for mail");
				}
				mailReceiver.waitForNewMessages();
				if (mailReceiver.getFolder().isOpen()) {
					Message[] mailMessages = mailReceiver.receive();
					if (logger.isDebugEnabled()) {
						logger.debug("received " + mailMessages.length + " mail messages");
					}
					for (final Message mailMessage : mailMessages) {

						Runnable messageSendingTask = createMessageSendingTask(mailMessage);

						sendingTaskExecutor.execute(messageSendingTask);
					}
				}
			}
			catch (MessagingException e) {
				if (logger.isWarnEnabled()) {
					logger.warn("error occurred in idle task", e);
				}
				if (shouldReconnectAutomatically) {
					throw new IllegalStateException(
							"Failure in 'idle' task. Will resubmit.", e);
				}
				else {
					throw new org.springframework.messaging.MessagingException(
							"Failure in 'idle' task. Will NOT resubmit.", e);
				}
			}
		}
	}

	private Runnable createMessageSendingTask(final Message mailMessage){
		Runnable sendingTask = new Runnable() {
			@Override
			public void run() {
				org.springframework.messaging.Message<?> message =
						ImapIdleChannelAdapter.this.getMessageBuilderFactory().withPayload(mailMessage).build();

				if (TransactionSynchronizationManager.isActualTransactionActive()) {
					if (transactionSynchronizationFactory != null){
						TransactionSynchronization synchronization =
								transactionSynchronizationFactory.create(ImapIdleChannelAdapter.this);
						TransactionSynchronizationManager.registerSynchronization(synchronization);
						if (synchronization instanceof IntegrationResourceHolderSynchronization) {
							IntegrationResourceHolder holder =
									((IntegrationResourceHolderSynchronization) synchronization).getResourceHolder();
							holder.setMessage(message);
						}
					}
				}
				sendMessage(message);
			}
		};

		// wrap in the TX proxy if necessary
		if (!CollectionUtils.isEmpty(adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(sendingTask);
			if (!CollectionUtils.isEmpty(adviceChain)) {
				for (Advice advice : adviceChain) {
					proxyFactory.addAdvice(advice);
				}
			}
			sendingTask = (Runnable) proxyFactory.getProxy(classLoader);
		}
		return sendingTask;
	}

	private void publishException(Exception e) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new ImapIdleExceptionEvent(e));
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No application event publisher for exception: " + e.getMessage());
			}
		}
	}

	private class PingTask implements Runnable {

		@Override
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
	}

	private class ExceptionAwarePeriodicTrigger implements Trigger {

		private volatile boolean delayNextExecution;


		@Override
		public Date nextExecutionTime(TriggerContext triggerContext) {
			if (delayNextExecution){
				delayNextExecution = false;
				return new Date(System.currentTimeMillis() + reconnectDelay);
			}
			else {
				return new Date(System.currentTimeMillis());
			}
		}

		public void delayNextExecution() {
			this.delayNextExecution = true;
		}
	}

	public class ImapIdleExceptionEvent extends MailIntegrationEvent {

		private static final long serialVersionUID = -5875388810251967741L;

		public ImapIdleExceptionEvent(Exception e) {
			super(ImapIdleChannelAdapter.this, e);
		}

	}
}
