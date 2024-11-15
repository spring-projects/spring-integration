/*
 * Copyright 2019-2024 the original author or authors.
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

package org.springframework.integration.handler.advice;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
public class RateLimiterRequestHandlerAdviceTests {

	private static final Duration REFRESH_PERIOD = Duration.ofMillis(500);

	@Autowired
	private MessageChannel requestChannel;

	@Autowired
	private PollableChannel resultChannel;

	@Autowired
	private RateLimiterRequestHandlerAdvice rateLimiterRequestHandlerAdvice;

	@Test
	void testRateLimiter() throws InterruptedException {
		Message<?> testMessage = new GenericMessage<>("test");
		this.requestChannel.send(testMessage);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> IntStream.range(0, 10).forEach(i -> this.requestChannel.send(testMessage)))
				.withCauseInstanceOf(RequestNotPermitted.class)
				.withMessageContaining("Rate limit exceeded for: ");

		long howLongToWait =
				this.rateLimiterRequestHandlerAdvice.getRateLimiter()
						.reservePermission();

		TimeUnit.NANOSECONDS.sleep(howLongToWait + REFRESH_PERIOD.toNanos());

		this.requestChannel.send(testMessage);

		assertThat(this.resultChannel.receive(10_000)).isNotNull();
		assertThat(this.resultChannel.receive(10_000)).isNotNull();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public RateLimiterRequestHandlerAdvice rateLimiterRequestHandlerAdvice() {
			return new RateLimiterRequestHandlerAdvice(
					RateLimiterConfig.custom()
							.timeoutDuration(Duration.ofMillis(100))
							.limitRefreshPeriod(REFRESH_PERIOD)
							.limitForPeriod(1)
							.build());
		}

		@Bean
		public PollableChannel resultChannel() {
			return new QueueChannel();
		}

		@ServiceActivator(inputChannel = "requestChannel", outputChannel = "resultChannel",
				adviceChain = "rateLimiterRequestHandlerAdvice")
		public String handleRequest(String payload) {
			return payload;
		}

	}

}
