/*
 * Copyright 2016-2018 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * The {@link AbstractMessageChannel} implementation for the
 * Reactive Streams {@link Publisher} based on the Project Reactor {@link Flux}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class FluxMessageChannel extends AbstractMessageChannel
		implements Publisher<Message<?>>, ReactiveStreamsSubscribableChannel {

	private final List<Subscriber<? super Message<?>>> subscribers = new ArrayList<>();

	private final Map<Publisher<Message<?>>, ConnectableFlux<Message<?>>> publishers = new ConcurrentHashMap<>();

	private final Flux<Message<?>> flux;

	private FluxSink<Message<?>> sink;

	private ErrorHandler errorHandler;

	public FluxMessageChannel() {
		this.flux =
				Flux.<Message<?>>create(emitter -> this.sink = emitter, FluxSink.OverflowStrategy.IGNORE)
						.errorStrategyContinue((e, m) -> this.errorHandler.handleError(e))
						.publish()
						.autoConnect();
	}

	/**
	 * Specify an {@link ErrorHandler} to handle errors during {@link Subscriber#onNext(Object)}.
	 * By default the {@link MessagePublishingErrorHandler} is used.
	 * @param errorHandler the callback to handle errors during {@link Subscriber#onNext(Object)}
	 * @since 5.1
	 */
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
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.state(this.subscribers.size() > 0,
				() -> "The [" + this + "] doesn't have subscribers to accept messages");
		this.sink.next(message);
		return true;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.subscribers.add(subscriber);

		this.flux.doOnCancel(() -> this.subscribers.remove(subscriber))
				.retry()
				.subscribe(subscriber);

		this.publishers.values().forEach(ConnectableFlux::connect);
	}

	@Override
	public void subscribeTo(Publisher<Message<?>> publisher) {
		ConnectableFlux<Message<?>> connectableFlux =
				Flux.from(publisher)
						.doOnComplete(() -> this.publishers.remove(publisher))
						.doOnNext(this::send)
						.publish();

		this.publishers.put(publisher, connectableFlux);

		if (!this.subscribers.isEmpty()) {
			connectableFlux.connect();
		}
	}

}
