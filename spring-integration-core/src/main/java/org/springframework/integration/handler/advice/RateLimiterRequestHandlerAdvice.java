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

import java.io.Serial;
import java.time.Duration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * An {@link AbstractRequestHandlerAdvice} extension for a rate limiting to service method calls.
 * The implementation is based on the
 * <a href="https://github.com/resilience4j/resilience4j#ratelimiter">Resilience4j</a>.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.2
 */
public class RateLimiterRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	public static final String DEFAULT_NAME = "RateLimiterRequestHandlerAdvice";

	private final RateLimiter rateLimiter;

	/**
	 * Construct an instance based on default rate limiter options
	 * and {@value #DEFAULT_NAME} as a rate limiter name.
	 * @see RateLimiter#ofDefaults
	 */
	public RateLimiterRequestHandlerAdvice() {
		this(RateLimiter.ofDefaults(DEFAULT_NAME));
	}

	/**
	 * Construct an instance based on default rate limiter options and provided name.
	 * @param name the name for the rate limiter.
	 */
	public RateLimiterRequestHandlerAdvice(String name) {
		this(RateLimiter.ofDefaults(name));
		Assert.hasText(name, "'name' must not be empty");
	}

	/**
	 * Construct an instance based on the provided {@link RateLimiter}.
	 * @param rateLimiter the {@link RateLimiter} to use.
	 */
	public RateLimiterRequestHandlerAdvice(RateLimiter rateLimiter) {
		Assert.notNull(rateLimiter, "'rateLimiter' must not be null");
		this.rateLimiter = rateLimiter;
	}

	/**
	 * Construct an instance based on the provided {@link RateLimiterConfig}
	 * and {@value #DEFAULT_NAME} as a rate limiter name.
	 * @param rateLimiterConfig the {@link RateLimiterConfig} to use.
	 */
	public RateLimiterRequestHandlerAdvice(RateLimiterConfig rateLimiterConfig) {
		this(rateLimiterConfig, DEFAULT_NAME);
	}

	/**
	 * Construct an instance based on the provided {@link RateLimiterConfig} and name.
	 * @param rateLimiterConfig the {@link RateLimiterConfig} to use.
	 * @param name the name for the rate limiter.
	 */
	public RateLimiterRequestHandlerAdvice(RateLimiterConfig rateLimiterConfig, String name) {
		Assert.notNull(rateLimiterConfig, "'rateLimiterConfig' must not be null");
		Assert.hasText(name, "'name' must not be empty");
		this.rateLimiter = RateLimiter.of(name, rateLimiterConfig);
	}

	/**
	 * Change the {@code limitForPeriod} option of the {@link #rateLimiter}.
	 * @param limitForPeriod the {@code limitForPeriod} to use.
	 * @see RateLimiter#changeLimitForPeriod(int)
	 */
	public void setLimitForPeriod(int limitForPeriod) {
		this.rateLimiter.changeLimitForPeriod(limitForPeriod);
	}

	/**
	 * Change the {@code timeoutDuration} option of the {@link #rateLimiter}.
	 * @param timeoutDuration the {@code timeoutDuration} to use.
	 * @see RateLimiter#changeTimeoutDuration(Duration)
	 */
	public void setTimeoutDuration(Duration timeoutDuration) {
		this.rateLimiter.changeTimeoutDuration(timeoutDuration);
	}

	/**
	 * Obtain the metrics from the rate limiter.
	 * @return the {@link RateLimiter.Metrics} from rate limiter.
	 * @see RateLimiter#getMetrics()
	 */
	public RateLimiter.Metrics getMetrics() {
		return this.rateLimiter.getMetrics();
	}

	/**
	 * Get the {@link RateLimiter} which is configured for this advice.
	 * @return the {@link RateLimiter} for this advice.
	 */
	public RateLimiter getRateLimiter() {
		return this.rateLimiter;
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		try {
			return RateLimiter.decorateSupplier(this.rateLimiter, callback::execute).get();
		}
		catch (RequestNotPermitted ex) {
			throw new RateLimitExceededException(message, "Rate limit exceeded for: " + target, ex);
		}
	}

	/**
	 * A {@link MessagingException} wrapper for the {@link RequestNotPermitted}
	 * with the {@code requestMessage} and {@code target} context.
	 */
	public static class RateLimitExceededException extends MessagingException {

		@Serial
		private static final long serialVersionUID = 1L;

		RateLimitExceededException(Message<?> message, String description, RequestNotPermitted cause) {
			super(message, description, cause);
		}

	}

}
