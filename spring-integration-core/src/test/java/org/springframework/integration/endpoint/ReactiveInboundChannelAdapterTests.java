/*
 * Copyright 2018-2021 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.1
 */
@SpringJUnitConfig
@DirtiesContext
public class ReactiveInboundChannelAdapterTests {

	@Autowired
	private FluxMessageChannel fluxChannel;

	@Autowired
	@Qualifier("counterEndpoint")
	private AbstractPollingEndpoint abstractPollingEndpoint;

	@Test
	public void testReactiveInboundChannelAdapter() {
		Flux<Integer> testFlux =
				Flux.from(this.fluxChannel)
						.map(Message::getPayload)
						.cast(Integer.class);

		StepVerifier.create(testFlux)
				.expectSubscription()
				.expectNoEvent(Duration.ofSeconds(1))
				.then(() -> abstractPollingEndpoint.setMaxMessagesPerPoll(-1))
				.expectNext(2, 4, 6, 8, 10, 12, 14, 16)
				.thenCancel()
				.verify(Duration.ofSeconds(10));
	}

	@Autowired
	private FluxMessageChannel fluxChannel2;

	@Test
	public void testTimeSupplierConsistency() {
		Flux<Long> testFlux =
				Flux.from(this.fluxChannel2)
						.map(Message::getPayload)
						.cast(Date.class)
				.map(Date::getTime);

		List<Long> dates = new ArrayList<>();

		StepVerifier.create(testFlux)
				.consumeNextWith(dates::add)
				.consumeNextWith(dates::add)
				.consumeNextWith(dates::add)
				.thenCancel()
				.verify(Duration.ofSeconds(10));

		assertThat(dates.get(1) - dates.get(0)).isGreaterThanOrEqualTo(1000);
		assertThat(dates.get(2) - dates.get(1)).isGreaterThanOrEqualTo(1000);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		public TaskExecutor taskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

		@Bean
		@InboundChannelAdapter(value = "fluxChannel",
				poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "0", taskExecutor = "taskExecutor"))
		@EndpointId("counterEndpoint")
		public Supplier<Integer> counterMessageSupplier() {
			return () -> {
				int i = counter().incrementAndGet();
				return i % 2 == 0 ? i : null;
			};
		}

		@Bean
		public MessageChannel fluxChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		@InboundChannelAdapter(value = "fluxChannel2", poller = @Poller(fixedDelay = "1000"))
		public Supplier<Date> timeSupplier() {
			return Date::new;
		}

		@Bean
		public MessageChannel fluxChannel2() {
			return new FluxMessageChannel();
		}

	}

}
