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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.lang.Nullable;

/**
 * Base interface for Integration managed components.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface IntegrationManagement extends NamedComponent, DisposableBean {

	String METER_PREFIX = "spring.integration.";

	String SEND_TIMER_NAME = METER_PREFIX + "send";

	String RECEIVE_COUNTER_NAME = METER_PREFIX + "receive";

	/**
	 * Enable logging or not.
	 * @param enabled dalse to disable.
	 */
	@ManagedAttribute(description = "Use to disable debug logging during normal message flow")
	default void setLoggingEnabled(boolean enabled) {
	}

	/**
	 * Return whether logging is enabled.
	 * @return true if enabled.
	 */
	@ManagedAttribute
	default boolean isLoggingEnabled() {
		return true;
	}

	default void setManagedName(String managedName) {
	}

	default String getManagedName() {
		return null;
	}

	default void setManagedType(String managedType) {
	}

	default String getManagedType() {
		return null;
	}

	/**
	 * Return the overrides.
	 * @return the overrides.
	 * @since 5.0
	 */
	@Nullable
	default ManagementOverrides getOverrides() {
		return null;
	}

	/**
	 * Inject a {@link MetricsCaptor}
	 * @param captor the captor.
	 * @since 5.0.4
	 */
	default void registerMetricsCaptor(MetricsCaptor captor) {
		// no op
	}

	@Override
	default void destroy() {
		// no op
	}

	/**
	 * Return this {@link IntegrationManagement} as its concrete type.
	 * @param <T> the type.
	 * @return this.
	 * @since 5.4
	 */
	@SuppressWarnings("unchecked")
	default <T> T getThisAs() {
		return (T) this;
	}

	/**
	 * Toggles to inform the management configurer to not set these properties since
	 * the user has manually configured them in a bean definition. If true, the
	 * corresponding property will not be set by the configurer.
	 *
	 * @since 5.0
	 */
	class ManagementOverrides {

		public boolean loggingConfigured; // NOSONAR

	}

}
