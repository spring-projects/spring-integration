/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler.advice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Rate limiter advice, which will ensure call to service is Rate limited within set rate limit.
 * Request calls breaching the rate limit will wait for time rate limit is not breached.
 *
 * @author Meherzad Lahewala
 * @since 5.1
 */
public class RequestHandlerRateLimiterAdvice extends AbstractRequestHandlerAdvice {

	private final AtomicInteger currentIndex;
	private final long maxDelayInMillis;
	private final long oneTimeUnitInMillis;
	private final AtomicLongArray queue;
	private final int rate;

	/**
	 * Create an instance of RequestHandlerRateLimiterAdvice with the provided arguments.
	 *
	 * @param rate the rate per time unit.
	 * @param maxDelay the max allowable delay in time units. Use -1 for no max delay.
	 * @param timeUnit the time unit.
	 */
	public RequestHandlerRateLimiterAdvice(int rate, int maxDelay, TimeUnit timeUnit) {
		Assert.isTrue(rate > 0, "Rate should be a positive integer");
		Assert.notNull(timeUnit, "Timeunit should be non null");
		this.currentIndex = new AtomicInteger(0);
		this.maxDelayInMillis = timeUnit.toMillis(maxDelay);
		this.oneTimeUnitInMillis = timeUnit.toMillis(1);
		this.queue = new AtomicLongArray(rate);
		this.rate = rate;
	}

	/**
	 * Create an instance of RequestHandlerRateLimiterAdvice with the provided arguments.
	 *
	 * @param rate the rate per time unit.
	 * @param timeUnit the time unit.
	 */
	public RequestHandlerRateLimiterAdvice(int rate, TimeUnit timeUnit) {
		this(rate, -1, timeUnit);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
		delayIfNecessary(target, message);
		return callback.execute();
	}

	private void delayIfNecessary(Object target, Message<?> message) throws InterruptedException {
		long timeToWait = -1;
		while (timeToWait == -1) {
			long now = System.currentTimeMillis();
			int index = this.currentIndex.getAndUpdate(value -> value + 1 < this.rate ? value + 1 : 0);
			long oldValue = this.queue.get(index);
			long waitTill;
			if (now < oldValue) {
				waitTill = oldValue + this.oneTimeUnitInMillis;
			}
			else {
				long diff = now - oldValue;
				waitTill = now + (diff < this.oneTimeUnitInMillis ? this.oneTimeUnitInMillis - diff : 0);
			}
			if (this.queue.compareAndSet(index, oldValue, waitTill)) {
				timeToWait = waitTill - now;
			}
		}
		if (this.maxDelayInMillis != -1 && timeToWait > this.maxDelayInMillis) {
			throw new RateLimiterMaxDelayException(message, "Rate limiter maxDelay exceeded for " + target);
		}
		if (logger.isDebugEnabled() && timeToWait > 0) {
			logger.debug(String.format("Sleeping for %s ms", timeToWait));
		}
		TimeUnit.MILLISECONDS.sleep(timeToWait);
	}


	/**
	 * An exception thrown when the max delay is triggered for rate limiter.
	 */
	public static final class RateLimiterMaxDelayException extends MessagingException {

		private static final long serialVersionUID = 1L;

		public RateLimiterMaxDelayException(Message<?> message, String description) {
			super(message, description);
		}
	}
}
