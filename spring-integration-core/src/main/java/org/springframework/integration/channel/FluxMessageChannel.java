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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;

/**
 * The {@link AbstractMessageChannel} implementation for the
 * Reactive Streams {@link Publisher} based on the Project Reactor {@link FluxProcessor}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class FluxMessageChannel extends AbstractMessageChannel
		implements Publisher<Message<?>>, FluxSubscribableChannel {

	private final List<Subscriber<? super Message<?>>> subscribers = new ArrayList<>();

	private final List<Publisher<Message<?>>> publishers = new CopyOnWriteArrayList<>();

	private final FluxProcessor<Message<?>, Message<?>> processor;

	private final FluxSink<Message<?>> sink;

	private volatile boolean upstreamSubscribed;

	public FluxMessageChannel() {
		this(DirectProcessor.create());
	}

	public FluxMessageChannel(FluxProcessor<Message<?>, Message<?>> processor) {
		Assert.notNull(processor, "'processor' must not be null");
		this.processor = processor;
		this.sink = processor.sink();
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		this.sink.next(message);
		return true;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.subscribers.add(subscriber);

		this.processor.doOnCancel(() -> FluxMessageChannel.this.subscribers.remove(subscriber))
				.subscribe(subscriber);

		if (!this.upstreamSubscribed) {
			this.publishers.forEach(this::doSubscribeTo);
		}
	}

	@Override
	public void subscribeTo(Flux<Message<?>> publisher) {
		this.publishers.add(publisher);
		if (!this.subscribers.isEmpty()) {
			doSubscribeTo(publisher);
		}
	}

	protected void doSubscribeTo(Publisher<Message<?>> publisher) {
		Flux.from(publisher)
				.doOnSubscribe(s -> FluxMessageChannel.this.upstreamSubscribed = true)
				.doOnComplete(() -> {
					FluxMessageChannel.this.publishers.remove(publisher);
					if (FluxMessageChannel.this.publishers.isEmpty()) {
						FluxMessageChannel.this.upstreamSubscribed = false;
					}
				})
				.subscribe(this.processor);
	}

}
