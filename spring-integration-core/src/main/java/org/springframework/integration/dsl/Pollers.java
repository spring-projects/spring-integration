/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * An utility class to provide {@link PollerSpec}s for
 * {@link org.springframework.integration.scheduling.PollerMetadata} configuration
 * variants.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Pollers {

	public static PollerSpec trigger(Trigger trigger) {
		return new PollerSpec(trigger);
	}

	public static PollerSpec fixedRate(long period) {
		return fixedRate(period, null);
	}

	public static PollerSpec fixedRate(long period, TimeUnit timeUnit) {
		return fixedRate(period, timeUnit, 0);
	}
	public static PollerSpec fixedRate(long period, long initialDelay) {
		return periodicTrigger(period, null, true, initialDelay);
	}

	public static PollerSpec fixedRate(long period, TimeUnit timeUnit, long initialDelay) {
		return periodicTrigger(period, timeUnit, true, initialDelay);
	}

	public static PollerSpec fixedDelay(long period) {
		return fixedDelay(period, null);
	}

	public static PollerSpec fixedDelay(long period, TimeUnit timeUnit) {
		return fixedDelay(period, timeUnit, 0);
	}

	public static PollerSpec fixedDelay(long period, long initialDelay) {
		return periodicTrigger(period, null, false, initialDelay);
	}

	public static PollerSpec fixedDelay(long period, TimeUnit timeUnit, long initialDelay) {
		return periodicTrigger(period, timeUnit, false, initialDelay);
	}

	private static PollerSpec periodicTrigger(long period, TimeUnit timeUnit, boolean fixedRate, long initialDelay) {
		PeriodicTrigger periodicTrigger = new PeriodicTrigger(period, timeUnit);
		periodicTrigger.setFixedRate(fixedRate);
		periodicTrigger.setInitialDelay(initialDelay);
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
