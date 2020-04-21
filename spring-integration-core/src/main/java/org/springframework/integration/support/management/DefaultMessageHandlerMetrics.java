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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation; use the full constructor to customize the moving averages.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class DefaultMessageHandlerMetrics extends AbstractMessageHandlerMetrics {

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;


	protected final AtomicLong activeCount = new AtomicLong(); // NOSONAR final

	protected final AtomicLong handleCount = new AtomicLong(); // NOSONAR final

	protected final AtomicLong errorCount = new AtomicLong(); // NOSONAR final

	protected final ExponentialMovingAverage duration; // NOSONAR final

	public DefaultMessageHandlerMetrics() {
		this(null);
	}

	/**
	 * Construct an instance with the default moving average window (10).
	 * @param name the name.
	 */
	public DefaultMessageHandlerMetrics(String name) {
		this(name, new ExponentialMovingAverage(DEFAULT_MOVING_AVERAGE_WINDOW, 1000000.));
	}

	/**
	 * Construct an instance with the supplied {@link ExponentialMovingAverage} calculating
	 * the duration of processing by the message handler (and any downstream synchronous
	 * endpoints).
	 * @param name the name.
	 * @param duration an {@link ExponentialMovingAverage} for calculating the duration.
	 * @since 4.2
	 */
	public DefaultMessageHandlerMetrics(String name, ExponentialMovingAverage duration) {
		super(name);
		this.duration = duration;
	}

	@Override
	public MetricsContext beforeHandle() {
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
			this.duration.append(System.nanoTime() - ((DefaultHandlerMetricsContext) context).start);
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

	protected static class DefaultHandlerMetricsContext implements MetricsContext {

		protected final long start; // NOSONAR final

		protected DefaultHandlerMetricsContext(long start) {
			this.start = start;
		}

	}

}
