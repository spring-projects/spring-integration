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
 * Abstract base class for handler metrics implementations.
 *
 * @author Gary Russell
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public abstract class AbstractMessageHandlerMetrics implements ConfigurableMetrics {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	protected final String name; // NOSONAR final

	private boolean fullStatsEnabled;

	public AbstractMessageHandlerMetrics(String name) {
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
	 * Begin a handle event.
	 * @return the context to be used in the {@link #afterHandle(MetricsContext, boolean)}.
	 */
	public abstract MetricsContext beforeHandle();

	/**
	 * End a handle event
	 * @param context the context from the previous {@link #beforeHandle()}.
	 * @param success true for success, false otherwise.
	 */
	public abstract void afterHandle(MetricsContext context, boolean success);

	public abstract void reset();

	public abstract long getHandleCountLong();

	public abstract int getHandleCount();

	public abstract int getErrorCount();

	public abstract long getErrorCountLong();

	public abstract double getMeanDuration();

	public abstract double getMinDuration();

	public abstract double getMaxDuration();

	public abstract double getStandardDeviationDuration();

	public abstract int getActiveCount();

	public abstract long getActiveCountLong();

	public abstract Statistics getDuration();

}
