/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMessageSourceProducerTests {

	@Test
	@DisabledIfEnvironmentVariable(named = "bamboo_buildKey", matches = ".*?",
			disabledReason = "Timing is too short for CI")
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

		stepVerifier.verify(Duration.ofSeconds(10));

		reactiveMessageSourceProducer.stop();

		assertThat(ackState.get()).isTrue();
	}

}
