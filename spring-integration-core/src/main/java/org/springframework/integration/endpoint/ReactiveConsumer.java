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

package org.springframework.integration.endpoint;

import java.util.Iterator;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

import reactor.core.Cancellation;
import reactor.core.Exceptions;
import reactor.core.Receiver;
import reactor.core.Trackable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;


/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveConsumer extends AbstractEndpoint {

	private final Operators.SubscriberAdapter<Message<?>, Message<?>> subscriber;

	private volatile Publisher<Message<?>> publisher;

	private ErrorHandler errorHandler;


	public ReactiveConsumer(MessageChannel inputChannel, Consumer<Message<?>> consumer) {
		this(inputChannel, new ConsumerSubscriber(consumer));
	}

	@SuppressWarnings("unchecked")
	public ReactiveConsumer(MessageChannel inputChannel, Subscriber<Message<?>> subscriber) {
		Assert.notNull(inputChannel);
		Assert.notNull(subscriber);

		if (inputChannel instanceof Publisher) {
			this.publisher = (Publisher<Message<?>>) inputChannel;
		}
		else {
			this.publisher = adaptToPublisher(inputChannel);
		}

		this.subscriber = new Operators.SubscriberAdapter<Message<?>, Message<?>>(subscriber) {

			@Override
			protected void doNext(Message<?> message) {
				try {
					super.doNext(message);
				}
				catch (Exception e) {
					ReactiveConsumer.this.errorHandler.handleError(e);
					doOnSubscriberError(e);
				}
			}

		};
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.errorHandler == null) {
			Assert.notNull(getBeanFactory(), "BeanFactory is required");
			this.errorHandler = new MessagePublishingErrorHandler(new BeanFactoryChannelResolver(getBeanFactory()));
		}
	}

	@Override
	protected void doStart() {
		this.publisher.subscribe(this.subscriber);
	}

	@Override
	protected void doStop() {
		this.subscriber.cancel();
	}

	public static Publisher<Message<?>> adaptToPublisher(MessageChannel inputChannel) {
		if (inputChannel instanceof SubscribableChannel) {
			return adaptSubscribableChannelToPublisher((SubscribableChannel) inputChannel);
		}
		else if (inputChannel instanceof PollableChannel) {
			return adaptPollableChannelToPublisher((PollableChannel) inputChannel);
		}
		else {
			throw new IllegalArgumentException("The 'inputChannel' must be an instance of SubscribableChannel or " +
					"PollableChannel, not: " + inputChannel);
		}
	}

	private static Publisher<Message<?>> adaptSubscribableChannelToPublisher(SubscribableChannel inputChannel) {
		return new SubscribableChannelPublisherAdapter(inputChannel);
	}

	private static Publisher<Message<?>> adaptPollableChannelToPublisher(PollableChannel inputChannel) {
		return new PollableChannelPublisherAdapter(inputChannel);
	}


	private final static class SubscribableChannelPublisherAdapter implements Publisher<Message<?>> {

		private final SubscribableChannel channel;

		SubscribableChannelPublisherAdapter(SubscribableChannel channel) {
			this.channel = channel;
		}

		@Override
		public void subscribe(Subscriber<? super Message<?>> subscriber) {
			Flux.
					<Message<?>>create(emitter -> {
								MessageHandler messageHandler = emitter::next;
								this.channel.subscribe(messageHandler);
								emitter.setCancellation(() -> this.channel.unsubscribe(messageHandler));
							},
							FluxSink.OverflowStrategy.IGNORE)
					.subscribe(subscriber);
		}

	}

	private final static class PollableChannelPublisherAdapter implements Publisher<Message<?>> {

		private final PollableChannel channel;


		PollableChannelPublisherAdapter(final PollableChannel channel) {
			this.channel = channel;
		}

		@Override
		public void subscribe(Subscriber<? super Message<?>> subscriber) {
			Iterator<Message<?>> messageIterator = new Iterator<Message<?>>() {

				private Message<?> next = null;

				@Override
				public Message<?> next() {
					Message<?> message = this.next;
					this.next = null;
					return message;
				}

				@Override
				public boolean hasNext() {
					if (this.next == null) {
						this.next = PollableChannelPublisherAdapter.this.channel.receive(0);
					}
					return this.next != null;
				}

			};

			Mono.<Message<?>>delayMillis(100)
					.repeat()
					.concatMap(value -> Flux.fromIterable(() -> messageIterator))
					.subscribe(subscriber);
		}

	}

	private static final class ConsumerSubscriber implements Subscriber<Message<?>>, Receiver, Cancellation, Trackable {

		private final Consumer<Message<?>> consumer;

		private Subscription subscription;

		ConsumerSubscriber(Consumer<Message<?>> consumer) {
			Assert.notNull(consumer);
			this.consumer = consumer;
		}

		@Override
		public void onSubscribe(Subscription s) {
			this.subscription = s;
			s.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Message<?> message) {
			this.consumer.accept(message);
		}

		@Override
		public void onError(Throwable t) {
			if (t == null) {
				throw Exceptions.argumentIsNullException();
			}
			onComplete();
			Operators.onErrorDropped(t);
		}

		@Override
		public void onComplete() {
			if (this.subscription != null) {
				this.subscription = null;
			}
		}

		@Override
		public Object upstream() {
			return this.subscription;
		}

		@Override
		public void dispose() {
			Subscription s = this.subscription;
			if (s != null) {
				this.subscription = null;
				s.cancel();
			}
		}

		@Override
		public long getCapacity() {
			return Long.MAX_VALUE;
		}

		@Override
		public boolean isStarted() {
			return this.subscription != null;
		}

		@Override
		public boolean isTerminated() {
			return false;
		}

	}

}
