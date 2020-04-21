/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.support.management;


/**
 * An implementation of {@link org.springframework.integration.support.management.MessageHandlerMetrics}
 * that aggregates the total response
 * time over a sample, to avoid fetching the system time twice for every message.
 *
 * @author Gary Russell
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class AggregatingMessageHandlerMetrics extends DefaultMessageHandlerMetrics {

	private static final int DEFAULT_SAMPLE_SIZE = 1000;

	private final int sampleSize;

	private long start;

	public AggregatingMessageHandlerMetrics() {
		this(null, DEFAULT_SAMPLE_SIZE);
	}

	/**
	 * Construct an instance with the default moving average window (10).
	 * @param name the name.
	 * @param sampleSize the sample size over which to aggregate the duration.
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
	 * @param sampleSize the sample size over which to aggregate the duration.
	 */
	public AggregatingMessageHandlerMetrics(String name, ExponentialMovingAverage duration, int sampleSize) {
		super(name, duration);
		this.sampleSize = sampleSize;
	}

	@Override
	public synchronized MetricsContext beforeHandle() {
		long count = this.handleCount.getAndIncrement();
		if (isFullStatsEnabled() && count % this.sampleSize == 0) {
			this.start = System.nanoTime();
		}
		this.activeCount.incrementAndGet();
		return new AggregatingHandlerMetricsContext(this.start, count + 1);
	}

	@Override
	public void afterHandle(MetricsContext context, boolean success) {
		this.activeCount.decrementAndGet();
		AggregatingHandlerMetricsContext aggregatingContext = (AggregatingHandlerMetricsContext) context;
		if (success) {
			if (isFullStatsEnabled() && aggregatingContext.newCount % this.sampleSize == 0) {
				this.duration.append(System.nanoTime() - aggregatingContext.start);
			}
		}
		else {
			this.errorCount.incrementAndGet();
		}
	}

	protected static class AggregatingHandlerMetricsContext extends DefaultHandlerMetricsContext {

		protected long newCount;

		public AggregatingHandlerMetricsContext(long start, long newCount) {
			super(start);
			this.newCount = newCount;
		}

	}

}
