/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

/**
 *
 * @param <T> the message payload type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class PublisherIntegrationFlow<T> extends StandardIntegrationFlow implements Publisher<Message<T>> {

	private static final Subscription NO_OP_SUBSCRIPTION = new Subscription() {

		@Override
		public void request(long n) {
		}

		@Override
		public void cancel() {
		}

	};

	private final Queue<Subscriber<? super Message<T>>> subscribers = new LinkedBlockingQueue<>();

	private final MessageChannel messageChannel;

	private final Executor executor;

	PublisherIntegrationFlow(Set<Object> integrationComponents, MessageChannel messageChannel, Executor executor) {
		super(integrationComponents);
		this.messageChannel = messageChannel;
		this.executor = executor;
		start();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void subscribe(Subscriber<? super Message<T>> subscriber) {
		if (!isRunning()) {
			//Reactive Streams Specification: https://github.com/reactive-streams/reactive-streams-jvm#1.4
			subscriber.onSubscribe(NO_OP_SUBSCRIPTION);
			subscriber.onError(
					new IllegalStateException("The Publisher must be started ('Lifecycle.start()') " +
							"before accepting subscription."));
			return;
		}

		this.subscribers.add(subscriber);
		if (this.messageChannel instanceof SubscribableChannel) {
			subscriber.onSubscribe(new MessageHandlerSubscription((Subscriber<Message<?>>) subscriber));
		}
		else if (this.messageChannel instanceof PollableChannel) {
			subscriber.onSubscribe(new PollableSubscription((Subscriber<Message<?>>) subscriber));
		}
		else {
			//Reactive Streams Specification: https://github.com/reactive-streams/reactive-streams-jvm#1.4
			subscriber.onSubscribe(NO_OP_SUBSCRIPTION);
			subscriber.onError(
					new IllegalStateException("Unsupported MessageChannel type ["
							+ this.messageChannel + "]. Must be 'SubscribableChannel' or 'PollableChannel'."));
		}
	}

	@Override
	public void stop() {
		super.stop();
		shutdown();
	}

	public void shutdown() {
		Subscriber<? super Message<T>> subscriber;
		while ((subscriber = this.subscribers.poll()) != null) {
			subscriber.onComplete();
		}
	}


	private abstract class SubscriberSubscription implements Subscription {

		final Subscriber<Message<?>> subscriber;

		volatile boolean terminated;

		SubscriberSubscription(Subscriber<Message<?>> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void request(long n) {
			//Reactive Streams Specification: https://github.com/reactive-streams/reactive-streams-jvm#3.9
			if (n <= 0L) {
				this.subscriber.onError(
						new IllegalArgumentException("Spec. Rule 3.9 - " +
								"Cannot request a non strictly positive number: " + n));
			}
			//Reactive Streams Specification:  https://github.com/reactive-streams/reactive-streams-jvm#3.6
			else if (!this.terminated && isRunning()) {
				onRequest(n);
			}
		}

		@Override
		public void cancel() {
			PublisherIntegrationFlow.this.subscribers.remove(this.subscriber);
			this.terminated = true;
		}

		protected abstract void onRequest(long n);

	}

	private final class MessageHandlerSubscription extends SubscriberSubscription implements MessageHandler {

		private final Queue<Long> pendingRequests = new LinkedBlockingQueue<>();

		private final AtomicReference<Long> currentRequest = new AtomicReference<>();

		private final AtomicLong count = new AtomicLong();

		private volatile boolean unbounded;

		MessageHandlerSubscription(Subscriber<Message<?>> subscriber) {
			super(subscriber);
		}

		@Override
		public void onRequest(long n) {
			if (n == Long.MAX_VALUE) {
				this.unbounded = true;
				this.pendingRequests.clear();
				this.currentRequest.set(null);
				this.count.set(0);
			}
			else if (!this.unbounded) {
				if (this.currentRequest.get() != null) {
					this.pendingRequests.offer(n);
				}
				else {
					this.currentRequest.set(n);
					this.count.set(0);
				}
			}
			((SubscribableChannel) PublisherIntegrationFlow.this.messageChannel).subscribe(this);
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			if (this.terminated || !PublisherIntegrationFlow.this.isRunning()) {
				((SubscribableChannel) PublisherIntegrationFlow.this.messageChannel).unsubscribe(this);
				throw new MessageDeliveryException(message);
			}

			if (this.unbounded) {
				this.subscriber.onNext(message);
			}
			else {
				if (this.currentRequest.get() == null || this.count.getAndIncrement() == this.currentRequest.get()) {
					this.currentRequest.set(this.pendingRequests.poll());
					this.count.set(0);
					if (this.currentRequest.get() == null) {
						((SubscribableChannel) PublisherIntegrationFlow.this.messageChannel).unsubscribe(this);
						throw new MessageDeliveryException(message);
					}
				}
				this.subscriber.onNext(message);
			}
		}

		@Override
		public void cancel() {
			((SubscribableChannel) PublisherIntegrationFlow.this.messageChannel).unsubscribe(this);
			super.cancel();
		}

	}


	private final class PollableSubscription extends SubscriberSubscription {

		PollableSubscription(Subscriber<Message<?>> subscriber) {
			super(subscriber);
		}

		@Override
		public void onRequest(final long n) {
			PublisherIntegrationFlow.this.executor.execute(() -> {
				if (n == Long.MAX_VALUE) {
					while (!terminated && isRunning()) {
						Message<?> receive =
								((PollableChannel) PublisherIntegrationFlow.this.messageChannel).receive(50);
						if (receive != null) {
							subscriber.onNext(receive);
						}
					}
				}
				else {
					long i = 0;
					while (!terminated && isRunning() && i < n) {
						Message<?> receive =
								((PollableChannel) PublisherIntegrationFlow.this.messageChannel).receive(50);
						if (receive != null) {
							subscriber.onNext(receive);
							i++;
						}
					}
				}
			});
		}

	}

}
