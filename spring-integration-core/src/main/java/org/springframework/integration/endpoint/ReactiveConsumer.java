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

import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;


/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveConsumer extends AbstractEndpoint {

	private final Subscriber<Message<?>> subscriber;

	private final Consumer<Message<?>> consumer;

	private volatile Flux<Message<?>> publisher;

	private volatile Subscription subscription;

	private ErrorHandler errorHandler;


	public ReactiveConsumer(MessageChannel inputChannel, Subscriber<Message<?>> subscriber) {
		this(inputChannel, subscriber, null);
		Assert.notNull(subscriber);

	}

	public ReactiveConsumer(MessageChannel inputChannel, Consumer<Message<?>> consumer) {
		this(inputChannel, null, consumer);
		Assert.notNull(consumer);
	}

	@SuppressWarnings("unchecked")
	private ReactiveConsumer(MessageChannel inputChannel, Subscriber<Message<?>> subscriber,
			Consumer<Message<?>> consumer) {
		Assert.notNull(inputChannel);

		Publisher<Message<?>> publisher;
		if (inputChannel instanceof Publisher) {
			publisher = (Publisher<Message<?>>) inputChannel;
		}
		else {
			publisher = adaptToPublisher(inputChannel);
		}

		this.publisher = Flux.from(publisher)
				.doOnError(t -> this.errorHandler.handleError(t)) // NPE if method reference
				.doOnSubscribe(s -> this.subscription = s)
				.retry();

		this.subscriber = subscriber;
		this.consumer = consumer;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.errorHandler == null) {
			Assert.notNull(getBeanFactory(), "BeanFactory is required");
			this.errorHandler = new MessagePublishingErrorHandler(
					new BeanFactoryChannelResolver(getBeanFactory()));
		}
	}

	@Override
	protected void doStart() {
		if (this.subscriber != null) {
			this.publisher.subscribe(this.subscriber);
		}
		else {
			this.publisher.subscribe(this.consumer);
		}
	}

	@Override
	protected void doStop() {
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	private Publisher<Message<?>> adaptToPublisher(MessageChannel inputChannel) {
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

	private Publisher<Message<?>> adaptSubscribableChannelToPublisher(SubscribableChannel inputChannel) {
		return new SubscribableChannelPublisherAdapter(inputChannel);
	}

	private Publisher<Message<?>> adaptPollableChannelToPublisher(PollableChannel inputChannel) {
		return null;
	}


	private final static class SubscribableChannelPublisherAdapter
			implements Publisher<Message<?>>, Subscriber<Message<?>>, Subscription {

		private final DirectProcessor<Message<?>> delegate = DirectProcessor.create();

		private final MessageHandler subscriberAdapter = this.delegate.connectSink()::accept;

		private final SubscribableChannel channel;

		private Subscriber<? super Message<?>> actualSubscriber;

		private Subscription actualSubscription;


		private SubscribableChannelPublisherAdapter(SubscribableChannel channel) {
			this.channel = channel;
		}

		@Override
		public void subscribe(Subscriber<? super Message<?>> subscriber) {
			this.actualSubscriber = subscriber;
			this.delegate.subscribe(this);
			this.channel.subscribe(this.subscriberAdapter);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.actualSubscription = subscription;
			this.actualSubscriber.onSubscribe(this);
		}

		@Override
		public void onNext(Message<?> message) {
			this.actualSubscriber.onNext(message);
		}

		@Override
		public void onError(Throwable t) {
			this.actualSubscriber.onError(t);
		}

		@Override
		public void onComplete() {
			this.actualSubscriber.onComplete();
		}

		@Override
		public void request(long n) {
			this.actualSubscription.request(n);
		}

		@Override
		public void cancel() {
			this.channel.unsubscribe(this.subscriberAdapter);
			this.actualSubscription.cancel();
		}

	}

}
