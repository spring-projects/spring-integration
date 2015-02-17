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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.support.management.ExponentialMovingAverage;
import org.springframework.integration.support.management.ExponentialMovingAverageRate;
import org.springframework.integration.support.management.ExponentialMovingAverageRatio;
import org.springframework.integration.support.management.Statistics;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Registers all message channels, and accumulates statistics about their performance. The statistics are then published
 * locally for other components to consume and publish remotely.
 *
 * @author Dave Syer
 * @author Helena Edelson
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public class ChannelSendMetrics {

	protected final Log logger = LogFactory.getLog(getClass());

	public static final long ONE_SECOND_SECONDS = 1;

	public static final long ONE_MINUTE_SECONDS = 60;

	public static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;

	private final ExponentialMovingAverage sendDuration = new ExponentialMovingAverage(
			DEFAULT_MOVING_AVERAGE_WINDOW);

	private final ExponentialMovingAverageRate sendErrorRate = new ExponentialMovingAverageRate(
			ONE_SECOND_SECONDS, ONE_MINUTE_SECONDS, DEFAULT_MOVING_AVERAGE_WINDOW);

	private final ExponentialMovingAverageRatio sendSuccessRatio = new ExponentialMovingAverageRatio(
			ONE_MINUTE_SECONDS, DEFAULT_MOVING_AVERAGE_WINDOW);

	private final ExponentialMovingAverageRate sendRate = new ExponentialMovingAverageRate(
			ONE_SECOND_SECONDS, ONE_MINUTE_SECONDS, DEFAULT_MOVING_AVERAGE_WINDOW);

	private final AtomicLong sendCount = new AtomicLong();

	private final AtomicLong sendErrorCount = new AtomicLong();

	private final String name;

	private volatile boolean fullStatsEnabled;


	public ChannelSendMetrics(String name) {
		this.name = name;
	}

	public void setFullStatsEnabled(boolean fullStatsEnabled) {
		this.fullStatsEnabled = fullStatsEnabled;
	}

	public void destroy() {
		if (logger.isDebugEnabled()) {
			logger.debug(sendDuration);
		}
	}

	public String getName() {
		return name;
	}

	public long beforeSend() {
		if (logger.isTraceEnabled()) {
			logger.trace("Recording send on channel(" + this.name + ")");
		}

		long start = 0;
		if (this.fullStatsEnabled) {
			start = System.currentTimeMillis();
			this.sendRate.increment();
		}
		this.sendCount.incrementAndGet();

		return start;
	}

	public void afterSend(long start, boolean result) {
		if (start > 0) {
			long now = System.currentTimeMillis();
			long elapsed = now - start;
			if (result && this.fullStatsEnabled) {
				sendSuccessRatio.success();
				sendDuration.append(elapsed);
			}
			else {
				if (this.fullStatsEnabled) {
	 				sendSuccessRatio.failure();
					sendErrorCount.incrementAndGet();
				}
				sendErrorRate.increment();
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Elapsed: " + this.name + ": " + elapsed);
			}
		}
	}

	public synchronized void reset() {
		sendDuration.reset();
		sendErrorRate.reset();
		sendSuccessRatio.reset();
		sendRate.reset();
		sendCount.set(0);
		sendErrorCount.set(0);
	}

	public int getSendCount() {
		return (int) sendCount.get();
	}

	public long getSendCountLong() {
		return sendCount.get();
	}

	public int getSendErrorCount() {
		return (int) sendErrorCount.get();
	}

	public long getSendErrorCountLong() {
		return sendErrorCount.get();
	}

	public double getTimeSinceLastSend() {
		return sendRate.getTimeSinceLastMeasurement();
	}

	public double getMeanSendRate() {
		return sendRate.getMean();
	}

	public double getMeanErrorRate() {
		return sendErrorRate.getMean();
	}

	public double getMeanErrorRatio() {
		return 1 - sendSuccessRatio.getMean();
	}

	public double getMeanSendDuration() {
		return sendDuration.getMean();
	}

	public double getMinSendDuration() {
		return sendDuration.getMin();
	}

	public double getMaxSendDuration() {
		return sendDuration.getMax();
	}

	public double getStandardDeviationSendDuration() {
		return sendDuration.getStandardDeviation();
	}

	public Statistics getSendDuration() {
		return sendDuration.getStatistics();
	}

	public Statistics getSendRate() {
		return sendRate.getStatistics();
	}

	public Statistics getErrorRate() {
		return sendErrorRate.getStatistics();
	}

	@Override
	public String toString() {
		return String.format("MessageChannelMonitor: [name=%s, sends=%d]", name, sendCount.get());
	}

}
