/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
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
