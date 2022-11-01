/*
 * Copyright 2016-2022 the original author or authors.
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
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * An utility class to provide {@link PollerSpec}s for
 * {@link org.springframework.integration.scheduling.PollerMetadata} configuration
 * variants.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public final class Pollers {

	public static PollerSpec trigger(Trigger trigger) {
		return new PollerSpec(trigger);
	}

	public static PollerSpec fixedRate(long period) {
		return fixedRate(Duration.ofMillis(period));
	}

	public static PollerSpec fixedRate(Duration period) {
		return periodicTrigger(period, true, null);
	}

	/**
	 * @deprecated since 6.0 in favor of {@link #fixedRate(Duration)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static PollerSpec fixedRate(long period, TimeUnit timeUnit) {
		return fixedRate(Duration.of(period, timeUnit.toChronoUnit()));
	}

	public static PollerSpec fixedRate(Duration period, Duration initialDelay) {
		return periodicTrigger(period, true, initialDelay);
	}

	public static PollerSpec fixedRate(long period, long initialDelay) {
		return fixedRate(Duration.ofMillis(period), Duration.ofMillis(initialDelay));
	}

	/**
	 * @deprecated since 6.0 in favor of {@link #fixedRate(Duration, Duration)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static PollerSpec fixedRate(long period, TimeUnit timeUnit, long initialDelay) {
		ChronoUnit chronoUnit = timeUnit.toChronoUnit();
		return fixedRate(Duration.of(period, chronoUnit), Duration.of(initialDelay, chronoUnit));
	}

	public static PollerSpec fixedDelay(Duration period) {
		return periodicTrigger(period, false, null);
	}

	public static PollerSpec fixedDelay(long period) {
		return fixedDelay(Duration.ofMillis(period));
	}

	public static PollerSpec fixedDelay(Duration period, Duration initialDelay) {
		return periodicTrigger(period, false, initialDelay);
	}

	/**
	 * @deprecated since 6.0 in favor of {@link #fixedDelay(Duration)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static PollerSpec fixedDelay(long period, TimeUnit timeUnit) {
		return fixedDelay(Duration.of(period, timeUnit.toChronoUnit()));
	}

	public static PollerSpec fixedDelay(long period, long initialDelay) {
		return fixedDelay(Duration.ofMillis(period), Duration.ofMillis(initialDelay));
	}

	/**
	 * @deprecated since 6.0 in favor of {@link #fixedDelay(Duration, Duration)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public static PollerSpec fixedDelay(long period, TimeUnit timeUnit, long initialDelay) {
		ChronoUnit chronoUnit = timeUnit.toChronoUnit();
		return fixedDelay(Duration.of(period, chronoUnit), Duration.of(initialDelay, chronoUnit));
	}

	private static PollerSpec periodicTrigger(Duration period, boolean fixedRate, @Nullable Duration initialDelay) {
		PeriodicTrigger periodicTrigger = new PeriodicTrigger(period);
		periodicTrigger.setFixedRate(fixedRate);
		if (initialDelay != null) {
			periodicTrigger.setInitialDelay(initialDelay);
		}
		return new PollerSpec(periodicTrigger);
	}

	public static PollerSpec cron(String cronExpression) {
		return cron(cronExpression, TimeZone.getDefault());
	}

	public static PollerSpec cron(String cronExpression, TimeZone timeZone) {
		return new PollerSpec(new CronTrigger(cronExpression, timeZone));
	}

	private Pollers() {
	}

}
