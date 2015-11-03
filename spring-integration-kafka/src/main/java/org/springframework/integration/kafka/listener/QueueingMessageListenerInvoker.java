/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.listener;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.util.ObjectUtils;

import reactor.core.processor.RingBufferProcessor;

/**
 * Invokes a delegate {@link MessageListener} for all the messages passed to it, storing
 * them in an internal queue.
 *
 * @author Marius Bogoevici
 * @author Stephane Maldini
 */
class QueueingMessageListenerInvoker {

	private static Log log = LogFactory.getLog(QueueingMessageListenerInvoker.class);

	private final MessageListener messageListener;

	private final AcknowledgingMessageListener acknowledgingMessageListener;

	private final OffsetManager offsetManager;

	private final ErrorHandler errorHandler;

	private final int capacity;

	private final boolean autoCommitOnError;

	private final ExecutorService executorService;

	private volatile RingBufferProcessor<KafkaMessage> ringBufferProcessor;

	private volatile CancelableSingleTaskExecutorService cancelableExecutorService;

	private volatile boolean running = false;

	private volatile CountDownLatch shutdownLatch;

	public QueueingMessageListenerInvoker(int capacity, final OffsetManager offsetManager, Object delegate,
			final ErrorHandler errorHandler, Executor executor, boolean autoCommitOnError) {
		this.capacity = capacity;
		this.autoCommitOnError = autoCommitOnError;
		if (delegate instanceof MessageListener) {
			this.messageListener = (MessageListener) delegate;
			this.acknowledgingMessageListener = null;
		}
		else if (delegate instanceof AcknowledgingMessageListener) {
			this.acknowledgingMessageListener = (AcknowledgingMessageListener) delegate;
			this.messageListener = null;
		}
		else {
			// it's neither, an exception will be thrown
			throw new IllegalArgumentException("Either a "
					+ MessageListener.class.getName() + " or a "
					+ AcknowledgingMessageListener.class.getName() + " must be provided");
		}
		this.offsetManager = offsetManager;
		this.errorHandler = errorHandler;
		if (executor != null) {
			this.executorService = new ExecutorServiceAdapter(new ConcurrentTaskExecutor(
					executor));
		}
		else {
			this.executorService = Executors.newSingleThreadExecutor();
		}
	}

	/**
	 * Add a message to the queue, waiting if the queue has reached its maximum capacity.
	 *
	 * @param message the KafkaMessage to add
	 */
	public void enqueue(KafkaMessage message) {
		if (this.running) {
			ringBufferProcessor.onNext(message);
		}
	}

	public synchronized void start() {
		if (!this.running) {
			this.running = true;
			// wraps the executor, allowing for the interruption of the processing thread
			// on shutdown without
			// stopping the executor service (which may be injected)
			cancelableExecutorService = new CancelableSingleTaskExecutorService(executorService);
			this.ringBufferProcessor = RingBufferProcessor.share(cancelableExecutorService, capacity);
			this.ringBufferProcessor.subscribe(new KafkaMessageDispatchingSubscriber());
		}
	}

	public synchronized void stop(long stopTimeout) {
		if (this.running) {
			this.running = false;
			if (ringBufferProcessor != null) {
				// cancel the current task
				shutdownLatch = new CountDownLatch(1);
				cancelableExecutorService.cancelTask();
				// allow the processor to complete, even if no more messages will be processed
				ringBufferProcessor.onComplete();
				ringBufferProcessor = null;
				try {
					shutdownLatch.await(stopTimeout, TimeUnit.MILLISECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					shutdownLatch = null;
				}
			}
		}
	}

	/**
	 * {@link ExecutorService} implementation that supports the execution of a single
	 * task, deferring to the wrapped instance. Allows for interrupting the
	 * {@link RingBufferProcessor}'s executing thread, in case it is blocking.
	 * @since 1.3
	 */
	private static class CancelableSingleTaskExecutorService extends
			AbstractExecutorService {

		private Future<?> submittedTask;

		private final ExecutorService executor;

		public CancelableSingleTaskExecutorService(ExecutorService executor) {
			this.executor = executor;
		}

		@Override
		public void execute(Runnable task) {
			if (submittedTask == null) {
				submittedTask = this.executor.submit(task);
			}
			else {
				throw new IllegalArgumentException("Cannot submit more than one task");
			}
		}

		@Override
		public void shutdown() {
			throw new IllegalStateException("Manual shutdown not supported");
		}

		@Override
		public List<Runnable> shutdownNow() {
			throw new IllegalStateException("Manual shutdown not supported");
		}

		@Override
		public boolean isShutdown() {
			return false;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit)
				throws InterruptedException {
			throw new IllegalStateException("Manual termination not supported");
		}

		private void cancelTask() {
			if (submittedTask != null) {
				submittedTask.cancel(true);
				submittedTask = null;
			}
		}
	}

	private class KafkaMessageDispatchingSubscriber implements Subscriber<KafkaMessage> {

		@Override
		public void onSubscribe(Subscription s) {
			s.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(KafkaMessage kafkaMessage) {
			if (running) {
				try {
					if (messageListener != null) {
						messageListener.onMessage(kafkaMessage);
						offsetManager.updateOffset(kafkaMessage.getMetadata()
								.getPartition(), kafkaMessage.getMetadata()
								.getNextOffset());
					}
					else {
						acknowledgingMessageListener.onMessage(kafkaMessage,
								new DefaultAcknowledgment(offsetManager, kafkaMessage));
					}
				}
				catch (Exception e) {
					// we handle errors here so that we make sure that offsets are handled
					// concurrently
					if (errorHandler != null) {
						errorHandler.handle(e, kafkaMessage);
						if (autoCommitOnError) {
							offsetManager.updateOffset(kafkaMessage.getMetadata()
									.getPartition(), kafkaMessage.getMetadata()
									.getNextOffset());
						}
					}
				}
			}
			else {
				if (log.isDebugEnabled()) {
					log.debug("Message discarded on shutdown (no offsets have been committed): "
							+ ObjectUtils.nullSafeToString(kafkaMessage));
				}
			}
		}

		@Override
		public void onError(Throwable t) {
			// ignore
		}

		@Override
		public void onComplete() {
			if (shutdownLatch != null) {
				shutdownLatch.countDown();
			}
		}
	}
}
