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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMessageSourceProducerTests {

	@Test
	void testReactiveMessageSourceProducing() {
		LinkedBlockingQueue<Integer> queue =
				IntStream.range(0, 10)
						.boxed()
						.collect(Collectors.toCollection(LinkedBlockingQueue::new));

		AtomicBoolean ackState = new AtomicBoolean();

		MessageSource<Integer> messageSource =
				() -> {
					Integer integer = queue.poll();
					if (integer == null) {
						try {
							Thread.sleep(200);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new IllegalStateException(e);
						}
						integer = 100;
					}
					return MessageBuilder.withPayload(integer)
							.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
									(AcknowledgmentCallback) status -> ackState.set(true))
							.build();
				};

		FluxMessageChannel outputChannel = new FluxMessageChannel();

		ReactiveMessageSourceProducer reactiveMessageSourceProducer = new ReactiveMessageSourceProducer(messageSource);
		reactiveMessageSourceProducer.setDelayWhenEmpty(Duration.ofMillis(10));
		reactiveMessageSourceProducer.setOutputChannel(outputChannel);
		reactiveMessageSourceProducer.setBeanFactory(mock(BeanFactory.class));
		reactiveMessageSourceProducer.afterPropertiesSet();

		StepVerifier stepVerifier =
				StepVerifier.create(
						Flux.from(outputChannel)
								.map(Message::getPayload)
								.cast(Integer.class))
						.expectNextSequence(
								IntStream.range(0, 10)
										.boxed()
										.collect(Collectors.toList()))
						.expectNoEvent(Duration.ofMillis(100))
						.expectNext(100)
						.thenCancel()
						.verifyLater();

		reactiveMessageSourceProducer.start();

		stepVerifier.verify();

		reactiveMessageSourceProducer.stop();

		assertThat(ackState.get()).isTrue();
	}

}
