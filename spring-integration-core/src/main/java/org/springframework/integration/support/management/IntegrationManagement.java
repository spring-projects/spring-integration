/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Base interface for Integration managed components.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface IntegrationManagement {

	String METER_PREFIX = "spring.integration.";

	String SEND_TIMER_NAME = METER_PREFIX + "send";

	String RECEIVE_COUNTER_NAME = METER_PREFIX + "receive";

	@ManagedAttribute(description = "Use to disable debug logging during normal message flow")
	void setLoggingEnabled(boolean enabled);

	@ManagedAttribute
	boolean isLoggingEnabled();

	@ManagedOperation
	void reset();

	@ManagedAttribute(description = "Enable message counting statistics")
	void setCountsEnabled(boolean countsEnabled);

	@ManagedAttribute
	boolean isCountsEnabled();

	/**
	 * Return the overrides.
	 * @return the overrides.
	 * @since 5.0
	 */
	ManagementOverrides getOverrides();

	/**
	 * Inject a {@link MetricsCaptor}
	 * @param captor the captor.
	 * @since 5.0.4
	 */
	default void registerMetricsCaptor(MetricsCaptor captor) {
		// no op
	}

	/**
	 * Toggles to inform the management configurer to not set these properties since
	 * the user has manually configured them in a bean definition. If true, the
	 * corresponding property will not be set by the configurer.
	 *
	 * @since 5.0
	 */
	class ManagementOverrides {

		public boolean loggingConfigured;

		public boolean countsConfigured;

		public boolean statsConfigured;

		public boolean metricsConfigured;

	}

}
