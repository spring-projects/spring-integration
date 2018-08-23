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

package org.springframework.integration.util;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;


/**
 * This is a dynamically changeable {@link Trigger}. It is based on the
 * {@link PeriodicTrigger} implementations. However, the fields of this dynamic
 * trigger are not final and the properties can be inspected and set via
 * explicit getters and setters. Changes to the trigger take effect after the
 * next execution.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 4.2
 */
public class DynamicPeriodicTrigger implements Trigger {

	private volatile Duration initialDuration = Duration.ofMillis(0);

	private volatile Duration duration;

	private volatile TimeUnit timeUnit = TimeUnit.MILLISECONDS;

	private volatile boolean fixedRate = false;

	/**
	 * Create a trigger with the given period in milliseconds. The underlying
	 * {@link TimeUnit} will be initialized to TimeUnit.MILLISECONDS.
	 * @param period Must not be negative
	 */
	public DynamicPeriodicTrigger(long period) {
		this(Duration.ofMillis(period));
	}

	/**
	 * Create a trigger with the given period and time unit. The time unit will
	 * apply not only to the period but also to any 'initialDelay' value, if
	 * configured on this Trigger later via {@link #setInitialDelay(long)}.
	 * @param period Must not be negative
	 * @param timeUnit Must not be null
	 * @deprecated in favor of {@link #DynamicPeriodicTrigger(Duration)}.
	 */
	@Deprecated
	public DynamicPeriodicTrigger(long period, TimeUnit timeUnit) {
		Assert.isTrue(period >= 0, "period must not be negative");
		Assert.notNull(timeUnit, "timeUnit must not be null");

		this.timeUnit = timeUnit;
		this.duration = Duration.ofMillis(this.timeUnit.toMillis(period));
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
	 * Specify the delay for the initial execution. It will be evaluated in
	 * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
	 * provided upon instantiation, the default is milliseconds.
	 * @param initialDelay the initial delay in milliseconds.
	 * @deprecated in favor of {@link #setInitialDuration(Duration)}.
	 */
	@Deprecated
	public void setInitialDelay(long initialDelay) {
		Assert.isTrue(initialDelay >= 0, "initialDelay must not be negative");
		this.initialDuration = Duration.ofMillis(this.timeUnit.toMillis(initialDelay));
	}

	/**
	 * Specify the delay for the initial execution. It will be evaluated in
	 * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
	 * provided upon instantiation, the default is milliseconds.
	 * @param initialDuration the initial delay in milliseconds.
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
	 * Return the period in milliseconds.
	 * @return the period.
	 * @deprecated in favor of {@link #getDuration()}.
	 */
	@Deprecated
	public long getPeriod() {
		return this.duration.toMillis();
	}

	/**
	 * Specify the period of the trigger. It will be evaluated in
	 * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
	 * provided upon instantiation, the default is milliseconds.
	 * @param period Must not be negative
	 * @deprecated in favor of {@link #setDuration(Duration)}.
	 */
	@Deprecated
	public void setPeriod(long period) {
		Assert.isTrue(period >= 0, "period must not be negative");
		this.duration = Duration.ofMillis(this.timeUnit.toMillis(period));
	}

	/**
	 * Get the time unit.
	 * @return the time unit.
	 * @deprecated - use {@link Duration} instead.
	 */
	@Deprecated
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

	/**
	 * Set the time unit.
	 * @param timeUnit the time unit.
	 * @deprecated - use {@link Duration} instead.
	 */
	@Deprecated
	public void setTimeUnit(TimeUnit timeUnit) {
		Assert.notNull(timeUnit, "timeUnit must not be null");
		this.timeUnit = timeUnit;
	}

	/**
	 * Get the initial delay in milliseconds.
	 * @return the initial delay.
	 * @deprecated in favor of {@link #getInitialDuration()}.
	 */
	@Deprecated
	public long getInitialDelay() {
		return this.initialDuration.toMillis();
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
	 * @return the the next schedule date.
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		if (triggerContext.lastScheduledExecutionTime() == null) {
			return new Date(System.currentTimeMillis() + this.initialDuration.toMillis());
		}
		else if (this.fixedRate) {
			return new Date(triggerContext.lastScheduledExecutionTime().getTime() + this.duration.toMillis());
		}
		return new Date(triggerContext.lastCompletionTime().getTime() + this.duration.toMillis());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.duration == null) ? 0 : this.duration.hashCode());
		result = prime * result + (this.fixedRate ? 1231 : 1237);
		result = prime * result + ((this.initialDuration == null) ? 0 : this.initialDuration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DynamicPeriodicTrigger other = (DynamicPeriodicTrigger) obj;
		if (this.duration == null) {
			if (other.duration != null) {
				return false;
			}
		}
		else if (!this.duration.equals(other.duration)) {
			return false;
		}
		if (this.fixedRate != other.fixedRate) {
			return false;
		}
		if (this.initialDuration == null) {
			if (other.initialDuration != null) {
				return false;
			}
		}
		else if (!this.initialDuration.equals(other.initialDuration)) {
			return false;
		}
		return true;
	}


}
