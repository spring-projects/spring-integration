/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class FluxAggregatorMessageHandler extends AbstractMessageProducingHandler {

	private final AtomicBoolean subscribed = new AtomicBoolean();

	private final Flux<Message<?>> aggregatorFlux;

	private CorrelationStrategy correlationStrategy =
			new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	private Predicate<Message<?>> boundaryTrigger;

	private Function<Message<?>, Integer> windowSizeFunction = FluxAggregatorMessageHandler::sequenceSizeHeader;

	private Function<Flux<Message<?>>, Flux<Flux<Message<?>>>> windowConfigurer;

	private Duration windowTimespan;

	private Function<Flux<Message<?>>, Mono<Message<?>>> combineFunction = this::messageForWindowFlux;

	private FluxSink<Message<?>> sink;

	public FluxAggregatorMessageHandler() {
		this.aggregatorFlux =
				Flux.<Message<?>>create(emitter -> this.sink = emitter, FluxSink.OverflowStrategy.BUFFER)
						.groupBy(this::groupBy)
						.flatMap((group) -> group.transform(this::releaseBy))
						.publish()
						.autoConnect();
	}

	private Object groupBy(Message<?> message) {
		return this.correlationStrategy.getCorrelationKey(message);
	}

	private Flux<Message<?>> releaseBy(Flux<Message<?>> groupFlux) {
		return groupFlux
				.transform(this.windowConfigurer != null ? this.windowConfigurer : this::applyWindowOptions)
				.flatMap((windowFlux) -> windowFlux.transform(this.combineFunction));
	}

	private Flux<Flux<Message<?>>> applyWindowOptions(Flux<Message<?>> groupFlux) {
		if (this.boundaryTrigger != null) {
			return groupFlux.windowUntil(this.boundaryTrigger);
		}
		return groupFlux
				.switchOnFirst((signal, group) -> {
					if (signal.hasValue()) {
						Integer maxSize = this.windowSizeFunction.apply(signal.get());
						if (maxSize != null) {
							if (this.windowTimespan != null) {
								return group.windowTimeout(maxSize, this.windowTimespan);
							}
							else {
								return group.window(maxSize);
							}
						}
						else {
							if (this.windowTimespan != null) {
								return group.window(this.windowTimespan);
							}
							else {
								return Flux.error(
										new IllegalStateException(
												"One of the 'boundaryTrigger', 'windowSizeFunction' or "
														+ "'windowTimespan' options must be configured or " +
														"'sequenceSize' header must be supplied in the messages " +
														"to aggregate."));
							}
						}
					}
					else {
						return Flux.just(group);
					}
				});
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null");
		this.correlationStrategy = correlationStrategy;
	}

	public void setCombineFunction(Function<Flux<Message<?>>, Mono<Message<?>>> combineFunction) {
		Assert.notNull(combineFunction, "'combineFunction' must not be null");
		this.combineFunction = combineFunction;
	}

	public void setWindowConfigurer(Function<Flux<Message<?>>, Flux<Flux<Message<?>>>> windowConfigurer) {
		this.windowConfigurer = windowConfigurer;
	}

	public void setBoundaryTrigger(Predicate<Message<?>> boundaryTrigger) {
		this.boundaryTrigger = boundaryTrigger;
	}

	public void setWindowSize(int windowSize) {
		setWindowSizeFunction((message) -> windowSize);
	}

	public void setWindowSizeFunction(Function<Message<?>, Integer> windowSizeFunction) {
		Assert.notNull(windowSizeFunction, "'windowSizeFunction' must not be null");
		this.windowSizeFunction = windowSizeFunction;
	}

	public void setWindowTimespan(Duration windowTimespan) {
		this.windowTimespan = windowTimespan;
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		if (this.subscribed.compareAndSet(false, true)) {
			MessageChannel outputChannel = getOutputChannel();
			if (outputChannel instanceof ReactiveStreamsSubscribableChannel) {
				((ReactiveStreamsSubscribableChannel) outputChannel).subscribeTo(this.aggregatorFlux);
			}
			else {
				this.aggregatorFlux.subscribe((messageToSend) -> produceOutput(messageToSend, messageToSend));
			}
		}

		this.sink.next(message);
	}

	private Mono<Message<?>> messageForWindowFlux(Flux<Message<?>> messageFlux) {
		Flux<Message<?>> window = messageFlux.publish().autoConnect();
		return window
				.next()
				.map((first) ->
						getMessageBuilderFactory()
								.withPayload(Flux.concat(Mono.just(first), window))
								.copyHeaders(first.getHeaders())
								.build());
	}

	private static Integer sequenceSizeHeader(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
	}

}
