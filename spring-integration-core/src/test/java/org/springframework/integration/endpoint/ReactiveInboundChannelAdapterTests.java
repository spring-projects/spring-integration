/*
 * Copyright 2018-2019 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
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
	private FluxMessageChannel fluxMessageChannel;

	@Test
	public void testReactiveInboundChannelAdapter() {
		Flux<Integer> testFlux =
				Flux.from(this.fluxMessageChannel)
						.map(Message::getPayload)
						.cast(Integer.class);

		StepVerifier.create(testFlux)
				.expectNext(2, 4, 6, 8, 10, 12, 14, 16)
				.thenCancel()
				.verify();
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
				poller = @Poller(fixedDelay = "100", maxMessagesPerPoll = "3", taskExecutor = "taskExecutor"))
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

	}

}
