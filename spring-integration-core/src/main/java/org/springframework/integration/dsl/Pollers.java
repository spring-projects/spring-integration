/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.dsl;

import java.time.Duration;
import java.util.TimeZone;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * A utility class to provide {@link PollerSpec}s for
 * {@link org.springframework.integration.scheduling.PollerMetadata} configuration
 * variants.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public final class Pollers {

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period.
	 * The "fixed rate" means that periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * @param period the fixed rate period (in milliseconds) to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedRate(long period) {
		return fixedRate(Duration.ofMillis(period));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period.
	 * The "fixed rate" means that periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * @param period the fixed rate period to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedRate(Duration period) {
		return periodicTrigger(period, true, null);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period
	 * and initial delay.
	 * The "fixed rate" means that periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * @param period the fixed rate period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedRate(Duration period, Duration initialDelay) {
		return periodicTrigger(period, true, initialDelay);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period
	 * and initial delay.
	 * The "fixed rate" means that periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * @param period the fixed rate period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedRate(long period, long initialDelay) {
		return fixedRate(Duration.ofMillis(period), Duration.ofMillis(initialDelay));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period.
	 * The "fixed delay" means that periodic interval should be measured between the
	 * scheduled tasks actual completion times.
	 * @param period the fixed delay period to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedDelay(Duration period) {
		return periodicTrigger(period, false, null);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period.
	 * The "fixed delay" means that periodic interval should be measured between the
	 * scheduled tasks actual completion times.
	 * @param period the fixed delay period to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedDelay(long period) {
		return fixedDelay(Duration.ofMillis(period));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and initial delay.
	 * The "fixed delay" means that periodic interval should be measured between the
	 * scheduled tasks actual completion times.
	 * @param period the fixed delay period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedDelay(Duration period, Duration initialDelay) {
		return periodicTrigger(period, false, initialDelay);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and initial delay.
	 * The "fixed delay" means that periodic interval should be measured between the
	 * scheduled tasks actual completion times.
	 * @param period the fixed delay period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @see PeriodicTrigger
	 */
	public static PollerSpec fixedDelay(long period, long initialDelay) {
		return fixedDelay(Duration.ofMillis(period), Duration.ofMillis(initialDelay));
	}

	private static PollerSpec periodicTrigger(Duration period, boolean fixedRate, @Nullable Duration initialDelay) {
		PeriodicTrigger periodicTrigger = new PeriodicTrigger(period);
		periodicTrigger.setFixedRate(fixedRate);
		if (initialDelay != null) {
			periodicTrigger.setInitialDelay(initialDelay);
		}
		return trigger(periodicTrigger);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided cron expression.
	 * @param cronExpression the cron to use.
	 * @return the {@link PollerSpec}
	 * @see CronTrigger
	 */
	public static PollerSpec cron(String cronExpression) {
		return cron(cronExpression, TimeZone.getDefault());
	}

	/**
	 * Create a {@link PollerSpec} based on the provided cron expression and {@link TimeZone}.
	 * @param cronExpression the cron to use.
	 * @param timeZone the {@link TimeZone} to use.
	 * @return the {@link PollerSpec}
	 * @see CronTrigger
	 */
	public static PollerSpec cron(String cronExpression, TimeZone timeZone) {
		return trigger(new CronTrigger(cronExpression, timeZone));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided {@link Trigger}.
	 * @param trigger the {@link Trigger} to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#trigger(Trigger)
	 */
	public static PollerSpec trigger(Trigger trigger) {
		return new PollerSpec(trigger);
	}

	private Pollers() {
	}

}
