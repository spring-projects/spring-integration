/*
 * Copyright 2009-2020 the original author or authors.
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
 * An implementation of {@link MessageChannelMetrics} that aggregates the total response
 * time over a sample, to avoid fetching the system time twice for every message.
 *
 * @author Gary Russell
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class AggregatingMessageChannelMetrics extends DefaultMessageChannelMetrics {

	private static final int DEFAULT_SAMPLE_SIZE = 1000;

	private final int sampleSize;

	private long start;

	public AggregatingMessageChannelMetrics() {
		this(null, DEFAULT_SAMPLE_SIZE);
	}

	/**
	 * Construct an instance with default metrics with {@code window=10, period=1 second,
	 * lapsePeriod=1 minute}.
	 * @param name the name.
	 * @param sampleSize the sample size over which to aggregate the duration.
	 */
	public AggregatingMessageChannelMetrics(String name, int sampleSize) {
		super(name);
		this.sampleSize = sampleSize;
	}

	/**
	 * Construct an instance with the supplied metrics. For proper representation of metrics, the
	 * supplied sendDuration must have a {@code factor=1000000.} and the the other arguments
	 * must be created with the {@code millis} constructor argument set to true.
	 * @param name the name.
	 * @param sendDuration an {@link ExponentialMovingAverage} for calculating the send duration.
	 * @param sendErrorRate an {@link ExponentialMovingAverageRate} for calculating the send error rate.
	 * @param sendSuccessRatio an {@link ExponentialMovingAverageRatio} for calculating the success ratio.
	 * @param sendRate an {@link ExponentialMovingAverageRate} for calculating the send rate.
	 * @param sampleSize the sample size over which to aggregate the duration.
	 */
	public AggregatingMessageChannelMetrics(String name, ExponentialMovingAverage sendDuration,
			ExponentialMovingAverageRate sendErrorRate, ExponentialMovingAverageRatio sendSuccessRatio,
			ExponentialMovingAverageRate sendRate, int sampleSize) {
		super(name, sendDuration, sendErrorRate, sendSuccessRatio, sendRate);
		this.sampleSize = sampleSize;
	}

	@Override
	public synchronized MetricsContext beforeSend() {
		long count = this.sendCount.getAndIncrement();
		if (isFullStatsEnabled() && count % this.sampleSize == 0) {
			this.start = System.nanoTime();
			this.sendRate.increment(this.start);
		}
		return new AggregatingChannelMetricsContext(this.start, count + 1);
	}

	@Override
	public void afterSend(MetricsContext context, boolean result) {
		AggregatingChannelMetricsContext aggregatingContext = (AggregatingChannelMetricsContext) context;
		long newCount = aggregatingContext.newCount;
		if (result) {
			if (isFullStatsEnabled() && newCount % this.sampleSize == 0) {
				long now = System.nanoTime();
				this.sendSuccessRatio.success(now);
				this.sendDuration.append(now - aggregatingContext.start);
			}
		}
		else {
			if (isFullStatsEnabled() && newCount % this.sampleSize == 0) {
				long now = System.nanoTime();
				this.sendSuccessRatio.failure(now);
				this.sendErrorRate.increment(now);
			}
			this.sendErrorCount.incrementAndGet();
		}
	}

	protected static class AggregatingChannelMetricsContext extends DefaultChannelMetricsContext {

		protected long newCount; // NOSONAR

		public AggregatingChannelMetricsContext(long start, long newCount) {
			super(start);
			this.newCount = newCount;
		}

	}

}
