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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import reactor.core.processor.RingBufferProcessor;

/**
 * Invokes a delegate {@link MessageListener} for all the messages passed to it, storing them
 * in an internal queue.
 *
 * @author Marius Bogoevici
 * @author Stephane Maldini
 */
class QueueingMessageListenerInvoker {

	private final MessageListener messageListener;

	private final AcknowledgingMessageListener acknowledgingMessageListener;

	private final OffsetManager offsetManager;

	private final ErrorHandler errorHandler;

	private final int capacity;

	private final ExecutorService executorService;

	private RingBufferProcessor<KafkaMessage> ringBufferProcessor;

	private volatile boolean running = false;

	private volatile CountDownLatch shutdownLatch;

	public QueueingMessageListenerInvoker(int capacity, final OffsetManager offsetManager, Object delegate,
										  final ErrorHandler errorHandler, Executor executor) {
		this.capacity = capacity;
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
			throw new IllegalArgumentException("Either a " + MessageListener.class.getName() + " or a "
					+ AcknowledgingMessageListener.class.getName() + " must be provided");
		}
		this.offsetManager = offsetManager;
		this.errorHandler = errorHandler;
		if (executor != null) {
			this.executorService = new ExecutorServiceAdapter(new ConcurrentTaskExecutor(executor));
		}
		else {
			this.executorService = null;
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
			ExecutorService service = executorService != null ? executorService : Executors.newSingleThreadExecutor();
			this.ringBufferProcessor = RingBufferProcessor.share(service, capacity);
			this.ringBufferProcessor.subscribe(new KafkaMessageDispatchingSubscriber());
		}
	}

	public synchronized void stop(long stopTimeout) {
		if (this.running) {
			this.running = false;
			if (ringBufferProcessor != null) {
				shutdownLatch = new CountDownLatch(1);
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

	private class KafkaMessageDispatchingSubscriber implements Subscriber<KafkaMessage> {

		@Override
		public void onSubscribe(Subscription s) {
			s.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(KafkaMessage kafkaMessage) {
			try {
				if (messageListener != null) {
					messageListener.onMessage(kafkaMessage);
				}
				else {
					acknowledgingMessageListener.onMessage(kafkaMessage, new DefaultAcknowledgment(offsetManager, kafkaMessage));
				}
			}
			catch (Exception e) {
				// we handle errors here so that we make sure that offsets are handled concurrently
				if (errorHandler != null) {
					errorHandler.handle(e, kafkaMessage);
				}
			}
			finally {
				if (messageListener != null) {
					offsetManager.updateOffset(kafkaMessage.getMetadata().getPartition(),
							kafkaMessage.getMetadata().getNextOffset());
				}
			}
		}

		@Override
		public void onError(Throwable t) {
			//ignore
		}

		@Override
		public void onComplete() {
			CountDownLatch latch = shutdownLatch;
			if (latch != null) {
				latch.countDown();
			}
		}
	}
}
