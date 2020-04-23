/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.util;

import java.time.Duration;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AckUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Utilities for adapting integration components to/from reactive types.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public final class IntegrationReactiveUtils {

	/**
	 * The subscriber context entry for {@link Flux#delayElements}
	 * from the {@link Mono#repeatWhenEmpty(Function)}.
	 */
	public static final String DELAY_WHEN_EMPTY_KEY = "DELAY_WHEN_EMPTY_KEY";

	/**
	 * A default delay before repeating an empty source {@link Mono} as 1 second {@link Duration}.
	 */
	public static final Duration DEFAULT_DELAY_WHEN_EMPTY = Duration.ofSeconds(1);

	private IntegrationReactiveUtils() {
	}

	/**
	 * Wrap a provided {@link MessageSource} into a {@link Flux} for pulling the on demand.
	 * When {@link MessageSource#receive()} returns {@code null}, the source {@link Mono}
	 * goes to the {@link Mono#repeatWhenEmpty} state and performs a {@code delay}
	 * based on the {@link #DELAY_WHEN_EMPTY_KEY} {@link Duration} entry in the subscriber context
	 * or falls back to 1 second duration.
	 * If a produced message has an
	 * {@link org.springframework.integration.IntegrationMessageHeaderAccessor#ACKNOWLEDGMENT_CALLBACK} header
	 * it is ack'ed in the {@link Mono#doOnSuccess} and nack'ed in the {@link Mono#doOnError}.
	 * @param messageSource the {@link MessageSource} to adapt.
	 * @param <T> the expected payload type.
	 * @return a {@link Flux} which pulls messages from the {@link MessageSource} on demand.
	 */
	public static <T> Flux<Message<T>> messageSourceToFlux(MessageSource<T> messageSource) {
		return Mono.
				<Message<T>>create(monoSink ->
						monoSink.onRequest(value ->
								monoSink.success(messageSource.receive())))
				.doOnSuccess((message) ->
						AckUtils.autoAck(StaticMessageHeaderAccessor.getAcknowledgmentCallback(message)))
				.doOnError(MessagingException.class,
						(ex) -> {
							Message<?> failedMessage = ex.getFailedMessage();
							if (failedMessage != null) {
								AckUtils.autoNack(StaticMessageHeaderAccessor.getAcknowledgmentCallback(failedMessage));
							}
						})
				.subscribeOn(Schedulers.boundedElastic())
				.repeatWhenEmpty((repeat) ->
						repeat.flatMap((increment) ->
								Mono.subscriberContext()
										.flatMap(ctx ->
												Mono.delay(ctx.getOrDefault(DELAY_WHEN_EMPTY_KEY,
														DEFAULT_DELAY_WHEN_EMPTY)))))
				.repeat()
				.retry();
	}

	/**
	 * Adapt a provided {@link MessageChannel} into a {@link Flux} source:
	 * - a {@link org.springframework.integration.channel.FluxMessageChannel}
	 * is returned as is because it is already a {@link Publisher};
	 * - a {@link SubscribableChannel} is subscribed with a {@link MessageHandler}
	 * for the {@link EmitterProcessor#onNext(Object)} which is returned from this method;
	 * - a {@link PollableChannel} is wrapped into a {@link MessageSource} lambda and reuses
	 * {@link #messageSourceToFlux(MessageSource)}.
	 * @param messageChannel the {@link MessageChannel} to adapt.
	 * @param <T> the expected payload type.
	 * @return a {@link Flux} which uses a provided {@link MessageChannel} as a source for events to publish.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Flux<Message<T>> messageChannelToFlux(MessageChannel messageChannel) {
		if (messageChannel instanceof Publisher) {
			return Flux.from((Publisher<Message<T>>) messageChannel);
		}
		else if (messageChannel instanceof SubscribableChannel) {
			return adaptSubscribableChannelToPublisher((SubscribableChannel) messageChannel);
		}
		else if (messageChannel instanceof PollableChannel) {
			return messageSourceToFlux(() -> (Message<T>) ((PollableChannel) messageChannel).receive(0));
		}
		else {
			throw new IllegalArgumentException("The 'messageChannel' must be an instance of Publisher, " +
					"SubscribableChannel or PollableChannel, not: " + messageChannel);
		}
	}

	private static <T> Flux<Message<T>> adaptSubscribableChannelToPublisher(SubscribableChannel inputChannel) {
		return Flux.defer(() -> {
			EmitterProcessor<Message<T>> publisher = EmitterProcessor.create(1);
			@SuppressWarnings("unchecked")
			MessageHandler messageHandler = (message) -> publisher.onNext((Message<T>) message);
			inputChannel.subscribe(messageHandler);
			return publisher
					.doOnCancel(() -> inputChannel.unsubscribe(messageHandler));
		});
	}

}
