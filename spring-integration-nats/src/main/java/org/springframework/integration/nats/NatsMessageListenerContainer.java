/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import io.nats.client.impl.NatsJetStreamPullSubscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.nats.exception.NatsException;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Single-threaded Message listener container using the Java {@link Subscription} supporting push
 * and pull delivery mode
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public class NatsMessageListenerContainer extends AbstractNatsMessageListenerContainer {

	private static final Log LOG = LogFactory.getLog(NatsMessageListenerContainer.class);

	private volatile CountDownLatch startLatch = new CountDownLatch(1);

	private Subscription subscription;

	private volatile ListenableFuture<?> listenerConsumerFuture;

	/**
	 * Construct an instance with the consumerFactory and default delivery mode PULL.
	 *
	 * @param pNatsConsumerFactory NatsConsumerFactory bean with required information to create
	 *                             subscription and start polling for message
	 */
	public NatsMessageListenerContainer(@NonNull final NatsConsumerFactory pNatsConsumerFactory) {
		super(pNatsConsumerFactory, NatsMessageDeliveryMode.PULL);
	}

	/**
	 * Construct an instance with the consumerFactory and delivery mode
	 *
	 * @param pNatsConsumerFactory     NatsConsumerFactory bean with required information to create
	 *                                 subscription and start polling for message
	 * @param pNatsMessageDeliveryMode option to provide the message delivery mode
	 */
	public NatsMessageListenerContainer(
			@NonNull final NatsConsumerFactory pNatsConsumerFactory,
			@NonNull final NatsMessageDeliveryMode pNatsMessageDeliveryMode) {
		super(pNatsConsumerFactory, pNatsMessageDeliveryMode);
	}

	/**
	 * Initializes subscription using consumerFactory based on Delivery mode
	 *
	 * @param pMessageHandler Target for messages, listener bean which will further process the
	 *                        message
	 * @throws JetStreamApiException – the request had an error related to the data
	 * @throws IOException           - covers various communication issues with the NATS server such as timeout
	 *                               or interruption
	 */
	public void createSubscription(@NonNull final MessageHandler pMessageHandler)
			throws JetStreamApiException, IOException {
		switch (this.natsMessageDeliveryMode) {
			case CORE_ASYNC:
				this.subscription =
						this.natsConsumerFactory.createAsyncCorePushSubscription(pMessageHandler);
				break;
			case PUSH_ASYNC:
				// this code block might consumption of message as soon as
				// subscription is made. think about moving to start block
				this.subscription = this.natsConsumerFactory.createAsyncPushSubscription(pMessageHandler);
				break;
			case PUSH_SYNC:
				this.subscription = this.natsConsumerFactory.createSyncPushSubscription();
				break;
			default:
				this.subscription = this.natsConsumerFactory.createSyncPullSubscription();
				break;
		}
	}

	/**
	 * Method to start subscription and then poll for messages from NATS server
	 */
	@Override
	public void doStart() {
		if (isRunning()) {
			return;
		}
		final MessageHandler messageHandlerLocal = this.messageHandler;
		if (messageHandlerLocal == null) {
			LOG.error("MessageHandler cannot be null");
			return;
		}
		// Create NATS Subscription (Consumer in NATS client API) based on the
		// delivery mode
		try {
			createSubscription(messageHandlerLocal);
		}
		catch (JetStreamApiException | IOException | RuntimeException e) {
			stop(true);
			final String message =
					"Subscription is not available to start the container for subject: "
							+ this.natsConsumerFactory.getConsumerProperties().getSubject();
			LOG.error(message, e);
			throw new NatsException(message, e);
		}
		setRunning(true);

		// Push asynchronous mode does not need below block execution to start a
		// new thread. Push mode threads are created and managed by NATS client
		// API
		if (NatsMessageDeliveryMode.PUSH_ASYNC.equals(this.natsMessageDeliveryMode)
				|| NatsMessageDeliveryMode.CORE_ASYNC.equals(this.natsMessageDeliveryMode)) {
			return;
		}

		final ListenerConsumer listenerConsumer = new ListenerConsumer(messageHandlerLocal);
		final AsyncListenableTaskExecutor consumerTaskExecutor =
				new SimpleAsyncTaskExecutor(getBeanName() + "-nats-C-");
		this.startLatch = new CountDownLatch(1);
		this.listenerConsumerFuture = consumerTaskExecutor.submitListenable(listenerConsumer);
		try {
			if (!this.startLatch.await(5000, TimeUnit.MILLISECONDS)) {
				LOG.error("Error during starting consumer thread");
			}
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(
					"Container bean "
							+ getBeanName()
							+ " started for subject: "
							+ this.natsConsumerFactory.getConsumerProperties().getSubject());
		}
	}

	private ConsumerProperties getConsumerProperties() {
		return this.natsConsumerFactory.getConsumerProperties();
	}

	/**
	 * Stop the container and unsubscribe from the subscription
	 */
	@Override
	protected void doStop(final Runnable callback) {
		if (isRunning()) {
			callback.run();
			if (this.listenerConsumerFuture != null) {
				this.listenerConsumerFuture.addCallback(
						new NatsMessageListenerContainer.StopCallback(callback));
			}
			setRunning(false);
			unsubscribe();
			if (LOG.isDebugEnabled()) {
				LOG.debug(
						"Container bean "
								+ getBeanName()
								+ " stopped for subject: "
								+ this.natsConsumerFactory.getConsumerProperties().getSubject());
			}
		}
	}

	private void publishConsumerStartingEvent() {
		this.startLatch.countDown();
		if (LOG.isDebugEnabled()) {
			final String logMessage =
					"Initiating subscription for subject: "
							+ this.natsConsumerFactory.getConsumerProperties().getSubject()
							+ " and starting NATS consumption using delivery mode: "
							+ NatsMessageListenerContainer.this.natsMessageDeliveryMode.name();
			LOG.debug(logMessage);
		}
	}

	/**
	 * Unsubscribe from subscription when stopping the container
	 */
	private void unsubscribe() {
		// for push async, unsubscribe should be done via Dispatcher
		if (NatsMessageDeliveryMode.PUSH_ASYNC == this.natsMessageDeliveryMode) {
			this.subscription.getDispatcher().unsubscribe(this.subscription);
			return;
		}
		this.subscription.unsubscribe();
	}

	/**
	 * Runnable class to start consumer polling using NATS subscription
	 */
	private final class ListenerConsumer implements SchedulingAwareRunnable {

		private final MessageHandler messageHandlerRef;

		private final ConsumerProperties consumerProperties = getConsumerProperties();

		private final int pullBatchSize = this.consumerProperties.getPullBatchSize();

		private final Duration consumerMaxWait = this.consumerProperties.getConsumerMaxWait();

		ListenerConsumer(@NonNull final MessageHandler pMessageHandler) {
			this.messageHandlerRef = pMessageHandler;
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

		@Override
		public void run() {
			publishConsumerStartingEvent();
			while (isRunning()) {
				try {
					pollAndInvokeHandler();
				}
				catch (final InterruptedException e) {
					LOG.error("Error during poller invocation ", e);
					Thread.currentThread().interrupt();
				}
				catch (final Exception e) {
					// Log exceptions and continue with polling
					LOG.error("Exception occured during message processing ", e);
				}
				catch (final Error e) {
					// 1. If this Error (Exception) happen it is about
					// absolutely abnormal behavior of application
					// 2. That is we decide to take following actions
					// 2.1 in any case to stop the polling thread of adapter
					// which belong to appropriate workflow
					// 2.2. Log the situation in very good prepared format (!)
					// so that alerting and monitoring will be able to recognise
					// this situation and raise alert
					LOG.error(
							"Error during poller invocation, stopping container bean for subject: "
									+ this.consumerProperties.getSubject(),
							e);
					stop(true);
					throw e;
				}
			}
		}

		private void pollAndInvokeHandler() throws InterruptedException {
			if (isRunning()) {
				if (NatsMessageListenerContainer.this.subscription
						instanceof NatsJetStreamPullSubscription) {
					doPollForPullSubscription();
				}
				else {
					doPollForPushSubscription();
				}
			}
		}

		private void doPollForPushSubscription() throws InterruptedException {
			final Message message =
					NatsMessageListenerContainer.this.subscription.nextMessage(this.consumerMaxWait);
			if (message != null) {
				this.messageHandlerRef.onMessage(message);
			}
		}

		private void doPollForPullSubscription() throws InterruptedException {
			final NatsJetStreamPullSubscription pullSubscription =
					(NatsJetStreamPullSubscription) NatsMessageListenerContainer.this.subscription;
			final Iterator<Message> iterator =
					pullSubscription.iterate(this.pullBatchSize, this.consumerMaxWait);
			while (iterator.hasNext()) {
				// process message
				final Message message = iterator.next();
				this.messageHandlerRef.onMessage(message);
			}
		}
	}

	/**
	 * Callback to log the success or failure while stopping the container
	 */
	private class StopCallback implements ListenableFutureCallback<Object> {

		private final Runnable callback;

		StopCallback(final Runnable pCallback) {
			this.callback = pCallback;
		}

		@Override
		public void onFailure(final Throwable e) {
			LOG.error("Error while stopping the container", e);
			if (this.callback != null) {
				this.callback.run();
			}
		}

		@Override
		public void onSuccess(final Object result) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(this + " stopped normally");
			}
			if (this.callback != null) {
				this.callback.run();
			}
		}
	}
}
