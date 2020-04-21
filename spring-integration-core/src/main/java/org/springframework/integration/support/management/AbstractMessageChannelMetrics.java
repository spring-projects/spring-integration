/*
 * Copyright 2015-2020 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for channel metrics implementations.
 *
 * @author Gary Russell
 *
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 **/
@Deprecated
public abstract class AbstractMessageChannelMetrics implements ConfigurableMetrics {

	private static final String DEPRECATION = "deprecation";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	protected final String name; // NOSONAR - final

	private volatile boolean fullStatsEnabled;

	/**
	 * Construct an instance with the provided name.
	 * @param name the name.
	 */
	public AbstractMessageChannelMetrics(String name) {
		this.name = name;
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
	@SuppressWarnings(DEPRECATION)
	public abstract MetricsContext beforeSend();

	/**
	 * End a send event. Note that implementations typically will not validate that the
	 * context is of the correct type and not null; callers should take care to ensure
	 * the context is the object returned by the previous {@link #beforeSend()} call.
	 * @param context the context.
	 * @param result true for success, false otherwise.
	 */
	public abstract void afterSend(@SuppressWarnings(DEPRECATION) MetricsContext context, boolean result);

	/**
	 * Reset all counters/statistics.
	 */
	public abstract void reset();

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

	@SuppressWarnings(DEPRECATION)
	public abstract Statistics getSendDuration();

	@SuppressWarnings(DEPRECATION)
	public abstract Statistics getSendRate();

	@SuppressWarnings(DEPRECATION)
	public abstract Statistics getErrorRate();

	public abstract void afterReceive();

	public abstract void afterError();

	public abstract int getReceiveCount();

	public abstract long getReceiveCountLong();

	public abstract int getReceiveErrorCount();

	public abstract long getReceiveErrorCountLong();

}
