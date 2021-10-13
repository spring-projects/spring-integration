/*
 * Copyright 2019-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.concurrent.Queues;

/**
 * @author Sergei Egorov
 * @author Artem Bilan
 *
 * @since 5.1.9
 */
class IntegrationReactiveUtilsTests {

	private static final Scheduler SCHEDULER = Schedulers.boundedElastic();

	@AfterAll
	static void tearDown() {
		SCHEDULER.dispose();
	}

	@Test
	void testBackpressureWithSubscribableChannel() {
		Disposable.Composite compositeDisposable = Disposables.composite();
		try {
			DirectChannel channel = new DirectChannel();
			int initialRequest = 10;
			StepVerifier.create(IntegrationReactiveUtils.messageChannelToFlux(channel), initialRequest)
					.expectSubscription()
					.then(() -> {
						compositeDisposable.add(
								SCHEDULER.schedule(() -> {
									while (true) {
										if (channel.getSubscriberCount() > 0) {
											channel.send(new GenericMessage<>("foo"));
										}
									}
								})
						);
					})
					.expectNextCount(initialRequest)
					.expectNoEvent(Duration.ofMillis(100))
					.thenCancel()
					.verify(Duration.ofSeconds(1));
		}
		finally {
			compositeDisposable.dispose();
		}
	}

	@Test
	void testOverproducingWithSubscribableChannel() {
		DirectChannel channel = new DirectChannel();

		Disposable.Composite compositeDisposable = Disposables.composite();
		AtomicInteger sendCount = new AtomicInteger();
		try {
			int initialRequest = 10;
			StepVerifier.create(IntegrationReactiveUtils.messageChannelToFlux(channel), initialRequest)
					.expectSubscription()
					.then(() ->
							compositeDisposable.add(
									SCHEDULER.schedule(() -> {
										while (true) {
											if (channel.getSubscriberCount() > 0) {
												channel.send(new GenericMessage<>("foo"));
												sendCount.incrementAndGet();
											}
										}
									})
							))
					.expectNextCount(initialRequest)
					.thenAwait(Duration.ofMillis(100))
					.thenCancel()
					.verify(Duration.ofSeconds(1));
		}
		finally {
			compositeDisposable.dispose();
		}

		assertThat(sendCount.get())
				.as("produced")
				.isLessThanOrEqualTo(Queues.SMALL_BUFFER_SIZE);
	}

	@Test
	void testPublisherPayloadWithNullChannel() throws InterruptedException {
		NullChannel nullChannel = new NullChannel();
		CountDownLatch publisherSubscribed = new CountDownLatch(1);
		Mono<Object> mono = Mono.empty().doOnSubscribe((s) -> publisherSubscribed.countDown());
		nullChannel.send(new GenericMessage<>(mono));
		assertThat(publisherSubscribed.await(10, TimeUnit.SECONDS)).isTrue();
	}


	@Test
	void testRetryOnMessagingExceptionOnly() {
		AtomicInteger retryAttempts = new AtomicInteger(3);
		AtomicReference<Throwable> finalException = new AtomicReference<>();
		MessageSource<?> messageSource =
				() -> {
					if (retryAttempts.getAndDecrement() > 0) {
						throw new MessagingException("retryable MessagingException");
					}
					else {
						throw new RuntimeException("non-retryable RuntimeException");
					}
				};

		StepVerifier.create(IntegrationReactiveUtils.messageSourceToFlux(messageSource).doOnError(finalException::set))
				.expectSubscription()
				.expectErrorMessage("non-retryable RuntimeException")
				.verify(Duration.ofSeconds(1));

		assertThat(retryAttempts.get()).isEqualTo(-1);
		assertThat(finalException.get()).hasMessage("non-retryable RuntimeException");
	}

}
