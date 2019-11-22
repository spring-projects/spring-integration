/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

/**
 * The {@link AbstractMessageChannel} implementation for the
 * Reactive Streams {@link Publisher} based on the Project Reactor {@link Flux}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Sergei Egorov
 *
 * @since 5.0
 */
public class FluxMessageChannel extends AbstractMessageChannel
		implements Publisher<Message<?>>, ReactiveStreamsSubscribableChannel {

	private final EmitterProcessor<Message<?>> processor;

	private final FluxSink<Message<?>> sink;

	private final AtomicBoolean subscribed = new AtomicBoolean();

	private final ReplayProcessor<Boolean> subscribedSignal = ReplayProcessor.create(1);

	public FluxMessageChannel() {
		this.processor = EmitterProcessor.create(1, false);
		this.sink = this.processor.sink(FluxSink.OverflowStrategy.BUFFER);
		this.subscribedSignal.doOnNext(this.subscribed::set).subscribe();
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.state(this.subscribed.get(),
				() -> "The [" + this + "] doesn't have subscribers to accept messages");
		this.sink.next(message);
		return true;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.processor.doOnSubscribe((s) -> this.subscribedSignal.onNext(true))
				.doFinally((s) -> this.subscribedSignal.onNext(this.processor.hasDownstreams()))
				.subscribe(subscriber);
	}

	@Override
	public void subscribeTo(Publisher<? extends Message<?>> publisher) {
		Flux.from(publisher)
				.delaySubscription(this.subscribedSignal.filter(Boolean::booleanValue).next())
				.publishOn(Schedulers.boundedElastic())
				.doOnNext((message) -> {
					try {
						send(message);
					}
					catch (Exception e) {
						logger.warn("Error during processing event: " + message, e);
					}
				})
				.subscribe();
	}

	@Override
	public void destroy() {
		this.subscribedSignal.onNext(false);
		this.processor.onComplete();
		super.destroy();
	}

}
