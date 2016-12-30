/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Operators;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ReactiveChannel extends AbstractMessageChannel
		implements Publisher<Message<?>>, ReactiveSubscribableChannel {

	private final List<Subscriber<? super Message<?>>> subscribers = new ArrayList<>();

	private final List<Publisher<Message<?>>> publishers = new CopyOnWriteArrayList<>();

	private final Processor<Message<?>, Message<?>> processor;

	private final BlockingSink<Message<?>> sink;

	private volatile boolean upstreamSubscribed;

	public ReactiveChannel() {
		this(DirectProcessor.create());
	}

	public ReactiveChannel(Processor<Message<?>, Message<?>> processor) {
		Assert.notNull(processor, "'processor' must not be null");
		this.processor = processor;
		this.sink = BlockingSink.create(this.processor);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.sink.submit(message, timeout) > -1;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.subscribers.add(subscriber);
		this.processor.subscribe(new Operators.SubscriberAdapter<Message<?>, Message<?>>(subscriber) {

			@Override
			protected void doCancel() {
				super.doCancel();
				ReactiveChannel.this.subscribers.remove(subscriber);
			}

		});

		if (!this.upstreamSubscribed) {
			this.publishers.forEach(this::doSubscribeTo);
		}
	}

	@Override
	public void subscribeTo(Publisher<Message<?>> publisher) {
		this.publishers.add(publisher);
		if (!this.subscribers.isEmpty()) {
			doSubscribeTo(publisher);
		}
	}

	private void doSubscribeTo(Publisher<Message<?>> publisher) {
		publisher.subscribe(new Operators.SubscriberAdapter<Message<?>, Message<?>>(this.processor) {

			@Override
			protected void doOnSubscribe(Subscription subscription) {
				super.doOnSubscribe(subscription);
				ReactiveChannel.this.upstreamSubscribed = true;
			}

			@Override
			protected void doComplete() {
				super.doComplete();
				ReactiveChannel.this.publishers.remove(publisher);
				if (ReactiveChannel.this.publishers.isEmpty()) {
					ReactiveChannel.this.upstreamSubscribed = false;
				}
			}

		});
	}

}
