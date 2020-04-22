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

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.integration.util.IntegrationReactiveUtils;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.concurrent.Queues;

/**
 * @author Sergei Egorov
 * @author Artem Bilan
 *
 * @since 5.1.9
 */
class MessageChannelReactiveUtilsTests {

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
								Schedulers.boundedElastic().schedule(() -> {
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
		channel.setCountsEnabled(true);

		Disposable.Composite compositeDisposable = Disposables.composite();
		AtomicInteger sendCount = new AtomicInteger();
		try {
			int initialRequest = 10;
			StepVerifier.create(IntegrationReactiveUtils.messageChannelToFlux(channel), initialRequest)
					.expectSubscription()
					.then(() ->
							compositeDisposable.add(
									Schedulers.boundedElastic().schedule(() -> {
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

}
