/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

import io.micrometer.core.instrument.Counter;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface MessageSourceMetrics extends IntegrationManagement {

	/**
	 * @return the number of successful handler calls
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Message Count")
	int getMessageCount();

	/**
	 * @return the number of successful handler calls
	 * @since 3.0
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Message Count")
	long getMessageCountLong();

	void setManagedName(String name);

	String getManagedName();

	void setManagedType(String source);

	String getManagedType();

	/**
	 * Set a micrometer counter to count messages produced.
	 * @param counter the counter.
	 * @since 5.0.2
	 * @deprecated in favor of built-in counter registration via {@code MeterRegistry} callbacks.
	 * Will be remove in the next release.
	 */
	@Deprecated
	default void setCounter(Counter counter) {
		// no op
	}

}
