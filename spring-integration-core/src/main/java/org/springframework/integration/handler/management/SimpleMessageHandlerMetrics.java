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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.support.management.ExponentialMovingAverage;
import org.springframework.integration.support.management.Statistics;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public class SimpleMessageHandlerMetrics {

	private static final Log logger = LogFactory.getLog(SimpleMessageHandlerMetrics.class);

	private static final int DEFAULT_MOVING_AVERAGE_WINDOW = 10;


	private final AtomicLong activeCount = new AtomicLong();

	private final AtomicLong handleCount = new AtomicLong();

	private final AtomicLong errorCount = new AtomicLong();

	private final ExponentialMovingAverage duration = new ExponentialMovingAverage(DEFAULT_MOVING_AVERAGE_WINDOW);

	private volatile String name;

	private volatile boolean fullStatsEnabled;

	public void setName(String name) {
		this.name = name;
	}

	public void setFullStatsEnabled(boolean fullStatsEnabled) {
		this.fullStatsEnabled = fullStatsEnabled;
	}

	public long beforeHandle(Message<?> message) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("messageHandler(" + this.name + ") message(" + message + ") :");
		}
		long start = 0;
		if (this.fullStatsEnabled) {
			start = System.currentTimeMillis();
		}
		this.handleCount.incrementAndGet();
		this.activeCount.incrementAndGet();
		return start;
	}

	public void afterHandle(long time, boolean success) {
		if (this.fullStatsEnabled && success) {
			this.duration.append(System.currentTimeMillis());
		}
		else if (!success) {
			this.errorCount.incrementAndGet();
		}
	}

	public synchronized void reset() {
		this.duration.reset();
		this.errorCount.set(0);
		this.handleCount.set(0);
	}

	public long getHandleCountLong() {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting Handle Count:" + this);
		}
		return this.handleCount.get();
	}

	public int getHandleCount() {
		return (int) getHandleCountLong();
	}

	public int getErrorCount() {
		return (int) this.errorCount.get();
	}

	public long getErrorCountLong() {
		return this.errorCount.get();
	}

	public double getMeanDuration() {
		return this.duration.getMean();
	}

	public double getMinDuration() {
		return this.duration.getMin();
	}

	public double getMaxDuration() {
		return this.duration.getMax();
	}

	public double getStandardDeviationDuration() {
		return this.duration.getStandardDeviation();
	}

	public int getActiveCount() {
		return (int) this.activeCount.get();
	}

	public long getActiveCountLong() {
		return this.activeCount.get();
	}

	public Statistics getDuration() {
		return this.duration.getStatistics();
	}

}
