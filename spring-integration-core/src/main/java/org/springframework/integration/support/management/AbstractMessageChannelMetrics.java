/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.support.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

/**
 * Abstract base class for channel metrics implementations.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public abstract class AbstractMessageChannelMetrics implements ConfigurableMetrics {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final String name;

	private final Timer timer;

	private final Counter errorCounter;

	private volatile boolean fullStatsEnabled;

	/**
	 * Construct an instance with the provided name.
	 * @param name the name.
	 */
	public AbstractMessageChannelMetrics(String name) {
		this(name, null, null);
	}

	/**
	 * Construct an instance with the provided name, timer and error counter.
	 * A non-null timer requires a non-null error counter. When a timer is provided,
	 * Micrometer metrics are used and the legacy metrics are not maintained.
	 * @param name the name.
	 * @param timer the timer.
	 * @param errorCounter the error counter.
	 * @since 5.0.2
	 */
	public AbstractMessageChannelMetrics(String name, Timer timer, Counter errorCounter) {
		if (timer != null) {
			Assert.notNull(errorCounter, "'errorCounter' cannot be null if a timer is provided");
		}
		this.name = name;
		this.timer = timer;
		this.errorCounter = errorCounter;
	}

	/**
	 * When false, simple counts are maintained; when true complete statistics
	 * are maintained.
	 * @param fullStatsEnabled true for complete statistics.
	 */
	public void setFullStatsEnabled(boolean fullStatsEnabled) {
		this.fullStatsEnabled = fullStatsEnabled;
	}

	protected boolean isFullStatsEnabled() {
		return this.fullStatsEnabled;
	}

	/**
	 * Begin a send event.
	 * @return the context to be used in a subsequent {@link #afterSend(MetricsContext, boolean)}
	 * call.
	 */
	public abstract MetricsContext beforeSend();

	/**
	 * End a send event. Note that implementations typically will not validate that the
	 * context is of the correct type and not null; callers should take care to ensure
	 * the context is the object returned by the previous {@link #beforeSend()} call.
	 * @param context the context.
	 * @param result true for success, false otherwise.
	 */
	public abstract void afterSend(MetricsContext context, boolean result);

	/**
	 * Reset all counters/statistics.
	 */
	public abstract void reset();

	/**
	 * Return the timer if Micrometer metrics are being used.
	 * @return the timer, or null to indicate Micrometer is not being used.
	 * @since 5.0.2
	 */
	@Nullable
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * Return the error counter if Micrometer metrics are being used.
	 * @return the counter or null if Micrometer is not being used.
	 * @since 5.0.2
	 */
	@Nullable
	public Counter getErrorCounter() {
		return this.errorCounter;
	}

	public abstract int getSendCount();

	public abstract long getSendCountLong();

	public abstract int getSendErrorCount();

	public abstract long getSendErrorCountLong();

	public abstract double getTimeSinceLastSend();

	public abstract double getMeanSendRate();

	public abstract double getMeanErrorRate();

	public abstract double getMeanErrorRatio();

	public abstract double getMeanSendDuration();

	public abstract double getMinSendDuration();

	public abstract double getMaxSendDuration();

	public abstract double getStandardDeviationSendDuration();

	public abstract Statistics getSendDuration();

	public abstract Statistics getSendRate();

	public abstract Statistics getErrorRate();

	public abstract void afterReceive();

	public abstract void afterError();

	public abstract int getReceiveCount();

	public abstract long getReceiveCountLong();

	public abstract int getReceiveErrorCount();

	public abstract long getReceiveErrorCountLong();

}
