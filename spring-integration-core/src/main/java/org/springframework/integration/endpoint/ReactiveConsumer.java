/*
 * Copyright 2016-2017 the original author or authors.
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

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.MessageChannelReactiveUtils;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.Receiver;
import reactor.core.Trackable;
import reactor.core.publisher.Operators;


/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveConsumer extends AbstractEndpoint {

	private final Operators.SubscriberAdapter<Message<?>, Message<?>> subscriber;

	private final Lifecycle lifecycleDelegate;

	private volatile Publisher<Message<?>> publisher;

	private ErrorHandler errorHandler;

	@SuppressWarnings("unchecked")
	public ReactiveConsumer(MessageChannel inputChannel, MessageHandler messageHandler) {
		this(inputChannel,
				messageHandler instanceof Subscriber
						? (Subscriber<Message<?>>) messageHandler
						: new MessageHandlerSubscriber(messageHandler));
	}

	@SuppressWarnings("unchecked")
	public ReactiveConsumer(MessageChannel inputChannel, Subscriber<Message<?>> subscriber) {
		Assert.notNull(inputChannel, "'inputChannel' must not be null");
		Assert.notNull(subscriber, "'subscriber' must not be null");

		Publisher<?> messagePublisher = MessageChannelReactiveUtils.toPublisher(inputChannel);
		this.publisher = (Publisher<Message<?>>) messagePublisher;

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
		this.lifecycleDelegate = subscriber instanceof Lifecycle ? (Lifecycle) subscriber : null;
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
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.start();
		}
		this.publisher.subscribe(this.subscriber);
	}

	@Override
	protected void doStop() {
		this.subscriber.cancel();
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.stop();
		}
	}


	private static final class MessageHandlerSubscriber
			implements Subscriber<Message<?>>, Receiver, Disposable, Trackable, Lifecycle {

		private final Consumer<Message<?>> consumer;

		private Subscription subscription;

		private MessageHandler messageHandler;

		MessageHandlerSubscriber(MessageHandler messageHandler) {
			Assert.notNull(messageHandler, "'messageHandler' must not be null");
			this.messageHandler = messageHandler;
			this.consumer = this.messageHandler::handleMessage;
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


		@Override
		public void start() {
			if (this.messageHandler instanceof Lifecycle) {
				((Lifecycle) this.messageHandler).start();
			}
		}

		@Override
		public void stop() {
			if (this.messageHandler instanceof Lifecycle) {
				((Lifecycle) this.messageHandler).stop();
			}
		}

		@Override
		public boolean isRunning() {
			return !(this.messageHandler instanceof Lifecycle) || ((Lifecycle) this.messageHandler).isRunning();
		}

	}

}
