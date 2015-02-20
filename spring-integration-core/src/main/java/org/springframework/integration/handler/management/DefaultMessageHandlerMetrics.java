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

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.integration.support.management.ExponentialMovingAverage;
import org.springframework.integration.support.management.MetricsContext;
import org.springframework.integration.support.management.Statistics;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public class DefaultMessageHandlerMetrics extends AbstractMessageHandlerMetrics {

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;


	private final AtomicLong activeCount = new AtomicLong();

	private final AtomicLong handleCount = new AtomicLong();

	private final AtomicLong errorCount = new AtomicLong();

	private final ExponentialMovingAverage duration = new ExponentialMovingAverage(DEFAULT_MOVING_AVERAGE_WINDOW);

	public DefaultMessageHandlerMetrics() {
		super(null);
	}

	public DefaultMessageHandlerMetrics(String name) {
		super(name);
	}


	@Override
	public MetricsContext beforeHandle(Message<?> message) {
		if (logger.isTraceEnabled()) {
			logger.trace("messageHandler(" + this.name + ") message(" + message + ") :");
		}
		long start = 0;
		if (isFullStatsEnabled()) {
			start = System.nanoTime();
		}
		this.handleCount.incrementAndGet();
		this.activeCount.incrementAndGet();
		return new DefaultHandlerMetricsContext(start);
	}

	@Override
	public void afterHandle(MetricsContext context, boolean success) {
		this.activeCount.decrementAndGet();
		if (isFullStatsEnabled() && success) {
			this.duration.appendNanos(System.nanoTime() - ((DefaultHandlerMetricsContext) context).start);
		}
		else if (!success) {
			this.errorCount.incrementAndGet();
		}
	}

	@Override
	public synchronized void reset() {
		this.duration.reset();
		this.errorCount.set(0);
		this.handleCount.set(0);
	}

	@Override
	public long getHandleCountLong() {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Handle Count:" + this);
		}
		return this.handleCount.get();
	}

	@Override
	public int getHandleCount() {
		return (int) getHandleCountLong();
	}

	@Override
	public int getErrorCount() {
		return (int) this.errorCount.get();
	}

	@Override
	public long getErrorCountLong() {
		return this.errorCount.get();
	}

	@Override
	public double getMeanDuration() {
		return this.duration.getMean();
	}

	@Override
	public double getMinDuration() {
		return this.duration.getMin();
	}

	@Override
	public double getMaxDuration() {
		return this.duration.getMax();
	}

	@Override
	public double getStandardDeviationDuration() {
		return this.duration.getStandardDeviation();
	}

	@Override
	public int getActiveCount() {
		return (int) this.activeCount.get();
	}

	@Override
	public long getActiveCountLong() {
		return this.activeCount.get();
	}

	@Override
	public Statistics getDuration() {
		return this.duration.getStatistics();
	}

	private static class DefaultHandlerMetricsContext implements MetricsContext {

		private final long start;

		public DefaultHandlerMetricsContext(long start) {
			this.start = start;
		}

	}

}
