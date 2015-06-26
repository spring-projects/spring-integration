/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.handler.management;

import org.springframework.integration.support.management.ExponentialMovingAverage;
import org.springframework.integration.support.management.MetricsContext;
import org.springframework.messaging.Message;

/**
 * Implementation that averages response times over a sample, to avoid fetching the system time
 * on every message.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class AggregatingMessageHandlerMetrics extends DefaultMessageHandlerMetrics {

	private static final int DEFAULT_SAMPLE_SIZE = 1000;

	private final int sampleSize;

	public AggregatingMessageHandlerMetrics() {
		this(null, DEFAULT_SAMPLE_SIZE);
	}

	/**
	 * Construct an instance with the default moving average window (10).
	 * @param name the name.
	 */
	public AggregatingMessageHandlerMetrics(String name, int sampleSize) {
		super(name);
		this.sampleSize = sampleSize;
	}

	/**
	 * Construct an instance with the supplied {@link ExponentialMovingAverage} calculating
	 * the duration of processing by the message handler (and any downstream synchronous
	 * endpoints).
	 * @param name the name.
	 * @param duration an {@link ExponentialMovingAverage} for calculating the duration.
	 * @since 4.2
	 */
	public AggregatingMessageHandlerMetrics(String name, ExponentialMovingAverage duration, int sampleSize) {
		super(name, duration);
		this.sampleSize = sampleSize;
	}

	@Override
	public synchronized MetricsContext beforeHandle(Message<?> message) {
		long start = 0;
		if (this.handleCount.getAndIncrement() % this.sampleSize == 0 && isFullStatsEnabled()) {
			start = System.nanoTime();
		}
		this.activeCount.incrementAndGet();
		return new DefaultHandlerMetricsContext(start);
	}

	@Override
	public void afterHandle(MetricsContext context, boolean success) {
		this.activeCount.decrementAndGet();
		long start = ((DefaultHandlerMetricsContext) context).start;
		if (success) {
			if (start > 0 && isFullStatsEnabled() && success) {
				this.duration.append(System.nanoTime() - start);
			}
		}
		else {
			this.errorCount.incrementAndGet();
		}
	}

}
