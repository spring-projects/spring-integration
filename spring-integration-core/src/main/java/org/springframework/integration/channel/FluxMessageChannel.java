/*
 * Copyright 2015-2025 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageChannel} implementation for the
 * Reactive Streams {@link Publisher} based on the Project Reactor {@link Flux}.
 * <p>
 * This class implements {@link Lifecycle} to control subscriptions to publishers
 * attached via {@link #subscribeTo(Publisher)}, when this channel is restarted.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Sergei Egorov
 *
 * @since 5.0
 */
public class FluxMessageChannel extends AbstractMessageChannel
		implements Publisher<Message<?>>, ReactiveStreamsSubscribableChannel, Lifecycle {

	private final Sinks.Many<Message<?>> sink = Sinks.many().multicast().onBackpressureBuffer(1, false);

	private final List<Publisher<? extends Message<?>>> sourcePublishers = new ArrayList<>();

	private volatile Disposable.Composite upstreamSubscriptions = Disposables.composite();

	private volatile boolean active = true;

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.state(this.active && this.sink.currentSubscriberCount() > 0,
				() -> "The [" + this + "] doesn't have subscribers to accept messages");
		long remainingTime = 0;
		if (timeout > 0) {
			remainingTime = timeout;
		}
		long parkTimeout = 10; // NOSONAR
		long parkTimeoutNs = TimeUnit.MILLISECONDS.toNanos(parkTimeout);
		while (this.active && !tryEmitMessage(message)) {
			remainingTime -= parkTimeout;
			if (timeout >= 0 && remainingTime <= 0) {
				return false;
			}
			LockSupport.parkNanos(parkTimeoutNs);
		}
		return true;
	}

	private boolean tryEmitMessage(Message<?> message) {
		Message<?> messageToEmit = message;
		ContextView contextView = IntegrationReactiveUtils.captureReactorContext();
		if (!contextView.isEmpty()) {
			messageToEmit = MutableMessageBuilder.fromMessage(message)
					.setHeader(IntegrationMessageHeaderAccessor.REACTOR_CONTEXT, contextView)
					.build();
		}

		if (this.active) {
			return switch (this.sink.tryEmitNext(messageToEmit)) {
				case OK -> true;
				case FAIL_NON_SERIALIZED, FAIL_OVERFLOW -> false;
				case FAIL_ZERO_SUBSCRIBER ->
						throw new IllegalStateException("The [" + this + "] doesn't have subscribers to accept messages");
				case FAIL_TERMINATED, FAIL_CANCELLED ->
						throw new IllegalStateException("Cannot emit messages into the cancelled or terminated sink: "
								+ this.sink);
			};
		}
		else {
			return false;
		}
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.sink.asFlux()
				.publish(1)
				.refCount()
				.subscribe(subscriber);
	}

	@Override
	public void start() {
		this.active = true;
		this.upstreamSubscriptions = Disposables.composite();
		this.sourcePublishers.forEach(this::doSubscribeTo);
	}

	@Override
	public void stop() {
		this.active = false;
		this.upstreamSubscriptions.dispose();
	}

	@Override
	public boolean isRunning() {
		return this.active;
	}

	private void disposeUpstreamSubscription(AtomicReference<Disposable> disposableReference) {
		Disposable disposable = disposableReference.get();
		if (disposable != null) {
			this.upstreamSubscriptions.remove(disposable);
			disposable.dispose();
		}
	}

	@Override
	public void subscribeTo(Publisher<? extends Message<?>> publisher) {
		this.sourcePublishers.add(publisher);
		doSubscribeTo(publisher);
	}

	private void doSubscribeTo(Publisher<? extends Message<?>> publisher) {
		Flux<Object> upstreamPublisher =
				Flux.from(publisher)
						.doOnComplete(() -> this.sourcePublishers.remove(publisher))
						.delaySubscription(
								Mono.fromCallable(this.sink::currentSubscriberCount)
										.filter((value) -> value > 0)
										.repeatWhenEmpty((repeat) ->
												this.active ? repeat.delayElements(Duration.ofMillis(100)) : repeat))
						.flatMap((message) ->
								Mono.just(message)
										.handle((messageToHandle, syncSink) -> sendReactiveMessage(messageToHandle))
										.contextWrite(StaticMessageHeaderAccessor.getReactorContext(message)))
						.contextCapture();

		addPublisherToSubscribe(upstreamPublisher);
	}

	private void addPublisherToSubscribe(Flux<?> publisher) {
		AtomicReference<Disposable> disposableReference = new AtomicReference<>();

		Disposable disposable =
				publisher
						.doOnTerminate(() -> disposeUpstreamSubscription(disposableReference))
						.subscribe();

		if (!disposable.isDisposed()) {
			if (this.upstreamSubscriptions.add(disposable)) {
				disposableReference.set(disposable);
			}
		}
	}

	private void sendReactiveMessage(Message<?> message) {
		Message<?> messageToSend = message;
		// We have just restored Reactor context, so no need in a header anymore.
		if (messageToSend.getHeaders().containsKey(IntegrationMessageHeaderAccessor.REACTOR_CONTEXT)) {
			messageToSend =
					MutableMessageBuilder.fromMessage(message)
							.removeHeader(IntegrationMessageHeaderAccessor.REACTOR_CONTEXT)
							.build();
		}
		try {
			if (!send(messageToSend)) {
				logger.warn(
						new MessageDeliveryException(messageToSend, "Failed to send message to channel '" + this),
						"Message was not delivered");
			}
		}
		catch (Exception ex) {
			logger.error(ex, LogMessage.format("Error during processing event: %s", messageToSend));
		}
	}

	@Override
	public void destroy() {
		this.active = false;
		this.upstreamSubscriptions.dispose();
		this.sourcePublishers.clear();
		this.sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofSeconds(1)));
		super.destroy();
	}

}
