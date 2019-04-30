/*
 * Copyright 2017-2019 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

/**
 * Utilities for adaptation {@link MessageChannel}s to the {@link Publisher}s.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class MessageChannelReactiveUtils {

	private MessageChannelReactiveUtils() {
		super();
	}

	@SuppressWarnings("unchecked")
	public static <T> Publisher<Message<T>> toPublisher(MessageChannel messageChannel) {
		if (messageChannel instanceof Publisher) {
			return (Publisher<Message<T>>) messageChannel;
		}
		else if (messageChannel instanceof SubscribableChannel) {
			return adaptSubscribableChannelToPublisher((SubscribableChannel) messageChannel);
		}
		else if (messageChannel instanceof PollableChannel) {
			return adaptPollableChannelToPublisher((PollableChannel) messageChannel);
		}
		else {
			throw new IllegalArgumentException("The 'messageChannel' must be an instance of Publisher, " +
					"SubscribableChannel or PollableChannel, not: " + messageChannel);
		}
	}

	private static <T> Publisher<Message<T>> adaptSubscribableChannelToPublisher(SubscribableChannel inputChannel) {
		return new SubscribableChannelPublisherAdapter<>(inputChannel);
	}

	private static <T> Publisher<Message<T>> adaptPollableChannelToPublisher(PollableChannel inputChannel) {
		return new PollableChannelPublisherAdapter<>(inputChannel);
	}


	private static final class SubscribableChannelPublisherAdapter<T> implements Publisher<Message<T>> {

		private final SubscribableChannel channel;

		SubscribableChannelPublisherAdapter(SubscribableChannel channel) {
			this.channel = channel;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void subscribe(Subscriber<? super Message<T>> subscriber) {
			Flux.
					<Message<?>>create(emitter -> {
								MessageHandler messageHandler = emitter::next;
								this.channel.subscribe(messageHandler);
								emitter.onCancel(() -> this.channel.unsubscribe(messageHandler));
							},
							FluxSink.OverflowStrategy.IGNORE)
					.subscribe((Subscriber<? super Message<?>>) subscriber);
		}

	}

	private static final class PollableChannelPublisherAdapter<T> implements Publisher<Message<T>> {

		private final PollableChannel channel;

		PollableChannelPublisherAdapter(final PollableChannel channel) {
			this.channel = channel;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void subscribe(Subscriber<? super Message<T>> subscriber) {
			Flux
					.<Message<T>>create(sink ->
									sink.onRequest(n -> {
										Message<?> m;
										while (!sink.isCancelled() && n-- > 0
												&& (m = this.channel.receive()) != null) { // NOSONAR
											sink.next((Message<T>) m);
										}
									}),
							FluxSink.OverflowStrategy.IGNORE)
					.subscribeOn(Schedulers.elastic())
					.subscribe(subscriber);
		}

	}

}
