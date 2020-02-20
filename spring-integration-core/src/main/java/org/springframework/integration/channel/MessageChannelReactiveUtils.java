/*
 * Copyright 2017-2020 the original author or authors.
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

import org.reactivestreams.Publisher;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
		return Flux.defer(() -> {
			EmitterProcessor<Message<T>> publisher = EmitterProcessor.create(1);
			@SuppressWarnings("unchecked")
			MessageHandler messageHandler = (message) -> publisher.onNext((Message<T>) message);
			inputChannel.subscribe(messageHandler);
			return publisher
					.doOnCancel(() -> inputChannel.unsubscribe(messageHandler));
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> Publisher<Message<T>> adaptPollableChannelToPublisher(PollableChannel inputChannel) {
		return Mono.fromCallable(() -> (Message<T>) inputChannel.receive(0))
				.subscribeOn(Schedulers.boundedElastic())
				.repeatWhenEmpty(it -> it.delayElements(Duration.ofMillis(100))) // NOSONAR - magic
				.repeat();
	}

}
