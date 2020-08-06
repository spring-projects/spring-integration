/*
 * Copyright 2019-2020 the original author or authors.
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
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * The {@link AbstractMessageProducingHandler} implementation for aggregation logic based
 * on Reactor's {@link Flux#groupBy} and {@link Flux#window} operators.
 * <p>
 * The incoming messages are emitted into a {@link FluxSink} provided by the
 * {@link Flux#create} initialized in the constructor.
 * <p>
 * The resulting windows for groups are wrapped into {@link Message}s for downstream
 * consumption.
 * <p>
 * If the {@link #getOutputChannel()} is not a {@link ReactiveStreamsSubscribableChannel}
 * instance, a subscription for the whole aggregating {@link Flux} is performed in the
 * {@link #start()} method.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class FluxAggregatorMessageHandler extends AbstractMessageProducingHandler implements ManageableLifecycle {

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

	private volatile Disposable subscription;

	/**
	 * Create an instance with a {@link Flux#create} and apply {@link Flux#groupBy} and {@link Flux#window}
	 * transformation into it.
	 */
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

	/**
	 * Configure a {@link CorrelationStrategy} to determine a group key from the incoming messages.
	 * By default a {@link HeaderAttributeCorrelationStrategy} is used against a
	 * {@link IntegrationMessageHeaderAccessor#CORRELATION_ID} header value.
	 * @param correlationStrategy the {@link CorrelationStrategy} to use.
	 */
	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null");
		this.correlationStrategy = correlationStrategy;
	}

	/**
	 * Configure a transformation {@link Function} to apply for a {@link Flux} window to emit.
	 * Requires a {@link Mono} result with a {@link Message} as value as a combination result
	 * of the incoming {@link Flux} for window.
	 * By default a {@link Flux} for window is fully wrapped into a message with headers copied
	 * from the first message in window. Such a {@link Flux} in the payload has to be subscribed
	 * and consumed downstream.
	 * @param combineFunction the {@link Function} to use for result windows transformation.
	 */
	public void setCombineFunction(Function<Flux<Message<?>>, Mono<Message<?>>> combineFunction) {
		Assert.notNull(combineFunction, "'combineFunction' must not be null");
		this.combineFunction = combineFunction;
	}

	/**
	 * Configure a {@link Predicate} for messages to determine a window boundary in the
	 * {@link Flux#windowUntil} operator.
	 * Has a precedence over any other window configuration options.
	 * @param boundaryTrigger the {@link Predicate} to use for window boundary.
	 * @see Flux#windowUntil(Predicate)
	 */
	public void setBoundaryTrigger(Predicate<Message<?>> boundaryTrigger) {
		this.boundaryTrigger = boundaryTrigger;
	}

	/**
	 * Specify a size for windows to close.
	 * Can be combined with the {@link #setWindowTimespan(Duration)}.
	 * @param windowSize the size for window to use.
	 * @see Flux#window(int)
	 * @see Flux#windowTimeout(int, Duration)
	 */
	public void setWindowSize(int windowSize) {
		setWindowSizeFunction((message) -> windowSize);
	}

	/**
	 * Specify a {@link Function} to determine a size for windows to close against the first message in group.
	 * Tne result of the function can be combined with the {@link #setWindowTimespan(Duration)}.
	 * By default an {@link IntegrationMessageHeaderAccessor#SEQUENCE_SIZE} header is consulted.
	 * @param windowSizeFunction the {@link Function} to use to determine a window size
	 * against a first message in the group.
	 * @see Flux#window(int)
	 * @see Flux#windowTimeout(int, Duration)
	 */
	public void setWindowSizeFunction(Function<Message<?>, Integer> windowSizeFunction) {
		Assert.notNull(windowSizeFunction, "'windowSizeFunction' must not be null");
		this.windowSizeFunction = windowSizeFunction;
	}

	/**
	 * Configure a {@link Duration} for closing windows periodically.
	 * Can be combined with the {@link #setWindowSize(int)} or {@link #setWindowSizeFunction(Function)}.
	 * @param windowTimespan the {@link Duration} to use for windows to close periodically.
	 * @see Flux#window(Duration)
	 * @see Flux#windowTimeout(int, Duration)
	 */
	public void setWindowTimespan(Duration windowTimespan) {
		this.windowTimespan = windowTimespan;
	}

	/**
	 * Configure a {@link Function} to apply a transformation into the grouping {@link Flux}
	 * for any arbitrary {@link Flux#window} options not covered by the simple options.
	 * Has a precedence over any other window configuration options.
	 * @param windowConfigurer the {@link Function} to apply any custom window transformation.
	 */
	public void setWindowConfigurer(Function<Flux<Message<?>>, Flux<Flux<Message<?>>>> windowConfigurer) {
		this.windowConfigurer = windowConfigurer;
	}

	@Override
	public String getComponentType() {
		return "flux-aggregator";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.aggregator;
	}

	@Override
	public void start() {
		if (this.subscribed.compareAndSet(false, true)) {
			MessageChannel outputChannel = getOutputChannel();
			if (outputChannel instanceof ReactiveStreamsSubscribableChannel) {
				((ReactiveStreamsSubscribableChannel) outputChannel).subscribeTo(this.aggregatorFlux);
			}
			else {
				this.subscription =
						this.aggregatorFlux.subscribe((messageToSend) -> produceOutput(messageToSend, messageToSend));
			}
		}
	}

	@Override
	public void stop() {
		if (this.subscribed.compareAndSet(true, false) && this.subscription != null) {
			this.subscription.dispose();
		}
	}

	@Override
	public boolean isRunning() {
		return this.subscribed.get();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Assert.state(isRunning(),
				"The 'FluxAggregatorMessageHandler' has not been started to accept incoming messages");

		this.sink.next(message);
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
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
