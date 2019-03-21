/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.MessageChannelReactiveUtils;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.router.MessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;


/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ReactiveStreamsConsumer extends AbstractEndpoint implements IntegrationConsumer {

	private final MessageChannel inputChannel;

	private final MessageHandler handler;

	private final Publisher<Message<Object>> publisher;

	private final Subscriber<Message<?>> subscriber;

	private final Lifecycle lifecycleDelegate;

	private ErrorHandler errorHandler;

	private volatile Subscription subscription;

	@SuppressWarnings("unchecked")
	public ReactiveStreamsConsumer(MessageChannel inputChannel, MessageHandler messageHandler) {
		this(inputChannel,
				messageHandler instanceof Subscriber
						? (Subscriber<Message<?>>) messageHandler
						: new MessageHandlerSubscriber(messageHandler));
	}

	public ReactiveStreamsConsumer(MessageChannel inputChannel, final Subscriber<Message<?>> subscriber) {
		this.inputChannel = inputChannel;
		Assert.notNull(inputChannel, "'inputChannel' must not be null");
		Assert.notNull(subscriber, "'subscriber' must not be null");

		if (inputChannel instanceof NullChannel && logger.isWarnEnabled()) {
			logger.warn("The consuming from the NullChannel does not have any effects: " +
					"it doesn't forward messages sent to it. A NullChannel is the end of the flow.");
		}

		this.publisher = MessageChannelReactiveUtils.toPublisher(inputChannel);
		this.subscriber = subscriber;
		this.lifecycleDelegate = subscriber instanceof Lifecycle ? (Lifecycle) subscriber : null;
		if (subscriber instanceof MessageHandlerSubscriber) {
			this.handler = ((MessageHandlerSubscriber) subscriber).messageHandler;
		}
		else if (subscriber instanceof MessageHandler) {
			this.handler = (MessageHandler) subscriber;
		}
		else {
			this.handler = this.subscriber::onNext;
		}
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public MessageChannel getInputChannel() {
		return this.inputChannel;
	}

	@Override
	public MessageChannel getOutputChannel() {
		if (this.handler instanceof MessageProducer) {
			return ((MessageProducer) this.handler).getOutputChannel();
		}
		else if (this.handler instanceof MessageRouter) {
			return ((MessageRouter) this.handler).getDefaultOutputChannel();
		}
		else {
			return null;
		}
	}

	@Override
	public MessageHandler getHandler() {
		return this.handler;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.errorHandler == null) {
			this.errorHandler = IntegrationContextUtils.getErrorHandler(getBeanFactory());
		}
	}

	@Override
	protected void doStart() {
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.start();
		}
		this.publisher.subscribe(new BaseSubscriber<Message<?>>() {

			private final Subscriber<Message<?>> delegate = ReactiveStreamsConsumer.this.subscriber;

			@Override
			public void hookOnSubscribe(Subscription s) {
				this.delegate.onSubscribe(s);
				ReactiveStreamsConsumer.this.subscription = s;
			}

			@Override
			public void hookOnNext(Message<?> message) {
				try {
					this.delegate.onNext(message);
				}
				catch (Exception e) {
					ReactiveStreamsConsumer.this.errorHandler.handleError(e);
					hookOnError(e);
				}
			}

			@Override
			public void hookOnError(Throwable t) {
				this.delegate.onError(t);
			}

			@Override
			public void hookOnComplete() {
				this.delegate.onComplete();
			}

		});
	}

	@Override
	protected void doStop() {
		if (this.subscription != null) {
			this.subscription.cancel();
		}
		if (this.lifecycleDelegate != null) {
			this.lifecycleDelegate.stop();
		}
	}


	private static final class MessageHandlerSubscriber
			implements CoreSubscriber<Message<?>>, Disposable, Lifecycle {

		private final Consumer<Message<?>> consumer;

		private Subscription subscription;

		private final MessageHandler messageHandler;

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
		}

		@Override
		public void onComplete() {
			dispose();
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
		public boolean isDisposed() {
			return this.subscription == null;
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
