/*
 * Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.channel.management;

import org.springframework.integration.support.management.ExponentialMovingAverage;
import org.springframework.integration.support.management.ExponentialMovingAverageRate;
import org.springframework.integration.support.management.ExponentialMovingAverageRatio;
import org.springframework.integration.support.management.MetricsContext;

/**
 * An implementation of {@link MessageChannelMetrics} that provides the total response
 * times over a sample, to avoid fetching the system time twice for every message.
 *
 * @author Gary Russell
 * @since 4.2
 */
public class AggregatingMessageChannelMetrics extends DefaultMessageChannelMetrics {

	private static final int DEFAULT_SAMPLE_SIZE = 1000;

	private final int sampleSize;

	public AggregatingMessageChannelMetrics() {
		this(null, DEFAULT_SAMPLE_SIZE);
	}

	/**
	 * Construct an instance with default metrics with {@code window=10, period=1 second,
	 * lapsePeriod=1 minute}.
	 * @param name the name.
	 * @param sampleSize the sample size
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
	 * @param sampleSize the sample size
	 */
	public AggregatingMessageChannelMetrics(String name, ExponentialMovingAverage sendDuration,
			ExponentialMovingAverageRate sendErrorRate, ExponentialMovingAverageRatio sendSuccessRatio,
			ExponentialMovingAverageRate sendRate, int sampleSize) {
		super(name, sendDuration, sendErrorRate, sendSuccessRatio, sendRate);
		this.sampleSize = sampleSize;
	}

	@Override
	public synchronized MetricsContext beforeSend() {
		long start = 0;
		long count = this.sendCount.getAndIncrement();
		if (isFullStatsEnabled() && count % this.sampleSize == 0) {
			start = System.nanoTime();
			this.sendRate.increment(start);
		}
		return new DefaultChannelMetricsContext(start);
	}

	@Override
	public void afterSend(MetricsContext context, boolean result) {
		long start = ((DefaultChannelMetricsContext) context).start;
		if (result) {
			if (start > 0 && isFullStatsEnabled()) {
				long now = System.nanoTime();
				this.sendSuccessRatio.success(now);
				this.sendDuration.append(now - start);
			}
		}
		else {
			if (start > 0 && isFullStatsEnabled()) {
				long now = System.nanoTime();
				this.sendSuccessRatio.failure(now);
				this.sendErrorRate.increment(now);
			}
			this.sendErrorCount.incrementAndGet();
		}
	}

}
