/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.Serial;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.mail.Folder;
import jakarta.mail.Message;
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
import org.springframework.messaging.MessagingException;
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
 * output channel. The Message payload will be the {@link jakarta.mail.Message}
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

	private List<Advice> adviceChain;

	private Consumer<Object> messageSender;

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
	 * Specify an {@link Executor} used to send messages received by the adapter.
	 * @param sendingTaskExecutor the sendingTaskExecutor to set
	 * @deprecated since 6.1 in favor of async hands-off downstream in the flow,
	 * e.g. {@link  org.springframework.integration.channel.ExecutorChannel}.
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public void setSendingTaskExecutor(Executor sendingTaskExecutor) {
	}

	/**
	 * Specify whether the IDLE task should reconnect automatically after
	 * catching a {@link jakarta.mail.MessagingException} while waiting for messages. The
	 * default value is true.
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

	@Override
	@SuppressWarnings("unchecked")
	protected void onInit() {
		super.onInit();

		Consumer<?> messageSenderToUse = new MessageSender();

		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(messageSenderToUse);
			this.adviceChain.forEach(proxyFactory::addAdvice);
			for (Advice advice : this.adviceChain) {
				proxyFactory.addAdvice(advice);
			}
			messageSenderToUse = (Consumer<?>) proxyFactory.getProxy(this.classLoader);
		}

		this.messageSender = (Consumer<Object>) messageSenderToUse;
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
	}

	private void publishException(Exception ex) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new ImapIdleExceptionEvent(ex));
		}
		else {
			logger.debug(() -> "No application event publisher for exception: " + ex.getMessage());
		}
	}

	private class MessageSender implements Consumer<Object> {

		MessageSender() {
		}

		@Override
		public void accept(Object mailMessage) {
			org.springframework.messaging.Message<?> messageToSend =
					mailMessage instanceof Message
							? getMessageBuilderFactory().withPayload(mailMessage).build()
							: (org.springframework.messaging.Message<?>) mailMessage;

			if (TransactionSynchronizationManager.isActualTransactionActive()
					&& ImapIdleChannelAdapter.this.transactionSynchronizationFactory != null) {

				TransactionSynchronization synchronization =
						ImapIdleChannelAdapter.this.transactionSynchronizationFactory.create(this);
				if (synchronization != null) {
					TransactionSynchronizationManager.registerSynchronization(synchronization);

					if (synchronization instanceof IntegrationResourceHolderSynchronization integrationSync
							&& !TransactionSynchronizationManager.hasResource(this)) {

						TransactionSynchronizationManager.bindResource(this, integrationSync.getResourceHolder());
					}

					Object resourceHolder = TransactionSynchronizationManager.getResource(this);
					if (resourceHolder instanceof IntegrationResourceHolder integrationResourceHolder) {
						integrationResourceHolder.setMessage(messageToSend);
					}
				}
			}
			sendMessage(messageToSend);
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
				catch (Exception ex) {
					if (ImapIdleChannelAdapter.this.shouldReconnectAutomatically
							&& ex.getCause() instanceof jakarta.mail.MessagingException messagingException) {

						//run again after a delay
						logger.info(messagingException,
								() -> "Failed to execute IDLE task. Will attempt to resubmit in "
										+ ImapIdleChannelAdapter.this.reconnectDelay + " milliseconds.");
						ImapIdleChannelAdapter.this.receivingTaskTrigger.delayNextExecution();
					}
					else {
						logger.warn(ex,
								"Failed to execute IDLE task. " +
										"Won't resubmit since not a 'shouldReconnectAutomatically'" +
										"or not a 'jakarta.mail.MessagingException'");
						ImapIdleChannelAdapter.this.receivingTaskTrigger.stop();
					}
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
							if (isRunning()) {
								ImapIdleChannelAdapter.this.messageSender.accept(mailMessage);
							}
						}
					}
				}
				catch (jakarta.mail.MessagingException ex) {
					logger.warn(ex, "error occurred in idle task");
					if (ImapIdleChannelAdapter.this.shouldReconnectAutomatically) {
						throw new IllegalStateException("Failure in 'idle' task. Will resubmit.", ex);
					}
					else {
						throw new MessagingException("Failure in 'idle' task. Will NOT resubmit.", ex);
					}
				}
			}
		}

	}

	private class ExceptionAwarePeriodicTrigger implements Trigger {

		private final AtomicBoolean delayNextExecution = new AtomicBoolean();

		private final AtomicBoolean stop = new AtomicBoolean();


		ExceptionAwarePeriodicTrigger() {
		}

		@Override
		public Instant nextExecution(TriggerContext triggerContext) {
			if (this.stop.getAndSet(false)) {
				return null;
			}
			if (this.delayNextExecution.getAndSet(false)) {
				return Instant.now().plusMillis(ImapIdleChannelAdapter.this.reconnectDelay);
			}
			else {
				return Instant.now();
			}
		}

		void delayNextExecution() {
			this.delayNextExecution.set(true);
		}

		void stop() {
			this.stop.set(true);
		}

	}

	public class ImapIdleExceptionEvent extends MailIntegrationEvent {

		@Serial
		private static final long serialVersionUID = -5875388810251967741L;

		ImapIdleExceptionEvent(Exception ex) {
			super(ImapIdleChannelAdapter.this, ex);
		}

	}

}
