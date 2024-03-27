/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * This is a dynamically changeable {@link Trigger}. It is based on the
 * {@link org.springframework.scheduling.support.PeriodicTrigger}
 * implementations. However, the fields of this dynamic
 * trigger are not final and the properties can be inspected and set via
 * explicit getters and setters. Changes to the trigger take effect after the
 * next execution.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class DynamicPeriodicTrigger implements Trigger {

	private Duration initialDuration = Duration.ofMillis(0);

	private Duration duration;

	private boolean fixedRate = false;

	/**
	 * Create a trigger with the given period in milliseconds.
	 * @param period Must not be negative
	 */
	public DynamicPeriodicTrigger(long period) {
		this(Duration.ofMillis(period));
	}

	/**
	 * Create a trigger with the provided duration.
	 * @param duration the duration.
	 * @since 5.1
	 */
	public DynamicPeriodicTrigger(Duration duration) {
		Assert.notNull(duration, "duration must not be null");
		Assert.isTrue(!duration.isNegative(), "duration must not be negative");
		this.duration = duration;
	}

	/**
	 * Specify the delay for the initial execution.
	 * @param initialDuration the initial delay.
	 * @since 5.1
	 */
	public void setInitialDuration(Duration initialDuration) {
		Assert.notNull(initialDuration, "initialDuration must not be null");
		Assert.isTrue(!initialDuration.isNegative(), "initialDuration must not be negative");
		this.initialDuration = initialDuration;
	}

	/**
	 * Return the duration.
	 * @return the duration.
	 * @since 5.1
	 */
	public Duration getDuration() {
		return this.duration;
	}

	/**
	 * Set the duration.
	 * @param duration the duration.
	 * @since 5.1
	 */
	public void setDuration(Duration duration) {
		Assert.notNull(duration, "duration must not be null");
		this.duration = duration;
	}

	/**
	 * Get the initial duration.
	 * @return the initial duration.
	 * @since 5.1
	 */
	public Duration getInitialDuration() {
		return this.initialDuration;
	}

	/**
	 * Specify whether the periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * The latter, "fixed delay" behavior, is the default.
	 * @param fixedRate the fixed rate {@code boolean} flag.
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * Return whether this trigger is fixed rate.
	 * @return the fixed rate.
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}

	/**
	 * Return the time after which a task should run again.
	 * @param triggerContext the trigger context to determine the previous state of schedule.
	 * @return the next schedule instant.
	 */
	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		Instant lastScheduled = triggerContext.lastScheduledExecution();
		if (lastScheduled == null) {
			return Instant.now().plus(this.initialDuration);
		}
		else if (this.fixedRate) {
			return lastScheduled.plus(this.duration);
		}
		return
				triggerContext.lastCompletion().plus(this.duration); // NOSONAR never null here
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DynamicPeriodicTrigger that = (DynamicPeriodicTrigger) obj;
		return this.fixedRate == that.fixedRate
				&& Objects.equals(this.initialDuration, that.initialDuration)
				&& Objects.equals(this.duration, that.duration);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.initialDuration, this.duration, this.fixedRate);
	}

}
