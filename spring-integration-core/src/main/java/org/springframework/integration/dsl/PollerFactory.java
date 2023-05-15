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

import org.springframework.scheduling.Trigger;

/**
 * An {@code Adapter} class for the {@link Pollers} factory.
 * Typically used with a Java 8 Lambda expression:
 * <pre class="code">
 * {@code
 *  c -> c.poller(p -> p.fixedRate(100))
 * }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class PollerFactory {

	/**
	 * Create a {@link PollerSpec} based on the provided {@link Trigger}.
	 * @param trigger the {@link Trigger} to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#trigger(Trigger)
	 */
	public PollerSpec trigger(Trigger trigger) {
		return Pollers.trigger(trigger);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided cron expression.
	 * @param cronExpression the cron to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#cron(String)
	 */
	public PollerSpec cron(String cronExpression) {
		return Pollers.cron(cronExpression);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided cron expression and {@link TimeZone}.
	 * @param cronExpression the cron to use.
	 * @param timeZone the {@link TimeZone} to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#cron(String, TimeZone)
	 */
	public PollerSpec cron(String cronExpression, TimeZone timeZone) {
		return Pollers.cron(cronExpression, timeZone);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period.
	 * @param period the fixed rate period to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#fixedRate(long)
	 */
	public PollerSpec fixedRate(long period) {
		return Pollers.fixedRate(period);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period and {@link TimeUnit}.
	 * @param period the fixed rate period to use.
	 * @param timeUnit the {@link TimeUnit} to use.
	 * @return the {@link PollerSpec}
	 * @deprecated since 6.1 in favor of {@link #fixedRate(Duration)}
	 * @see Pollers#fixedRate(Duration)
	 */
	@Deprecated(forRemoval = true)
	public PollerSpec fixedRate(long period, TimeUnit timeUnit) {
		return Pollers.fixedRate(Duration.of(period, timeUnit.toChronoUnit()));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period.
	 * @param period the fixed rate period to use.
	 * @return the {@link PollerSpec}
	 * @since 6.1
	 * @see Pollers#fixedRate(Duration)
	 */
	public static PollerSpec fixedRate(Duration period) {
		return Pollers.fixedRate(period);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period and initial delay.
	 * @param period the fixed rate period (in milliseconds) to use.
	 * @param initialDelay the initial delay (in milliseconds) to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#fixedRate(long, long)
	 */
	public PollerSpec fixedRate(long period, long initialDelay) {
		return Pollers.fixedRate(period, initialDelay);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period and initial delay .
	 * @param period the fixed rate period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @since 6.1
	 * @see Pollers#fixedRate(Duration)
	 */
	public static PollerSpec fixedRate(Duration period, Duration initialDelay) {
		return Pollers.fixedRate(period, initialDelay);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed rate period and {@link TimeUnit}
	 * with an initial delay.
	 * @param period the fixed rate period to use.
	 * @param timeUnit the {@link TimeUnit} to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @deprecated since 6.1 in favor of {@link #fixedRate(Duration, Duration)}
	 * @see Pollers#fixedRate(Duration, Duration)
	 */
	@Deprecated(forRemoval = true)
	public PollerSpec fixedRate(long period, TimeUnit timeUnit, long initialDelay) {
		ChronoUnit chronoUnit = timeUnit.toChronoUnit();
		return Pollers.fixedRate(Duration.of(period, chronoUnit), Duration.of(initialDelay, chronoUnit));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and {@link TimeUnit}
	 * with an initial delay.
	 * @param period the fixed delay period to use.
	 * @param timeUnit the {@link TimeUnit} to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @deprecated since 6.1 in favor of {@link #fixedDelay(Duration, Duration)}
	 * @see Pollers#fixedDelay(Duration, Duration)
	 */
	@Deprecated(forRemoval = true)
	public PollerSpec fixedDelay(long period, TimeUnit timeUnit, long initialDelay) {
		ChronoUnit chronoUnit = timeUnit.toChronoUnit();
		return Pollers.fixedDelay(Duration.of(period, chronoUnit), Duration.of(initialDelay, chronoUnit));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and {@link TimeUnit}.
	 * @param period the fixed delay period to use.
	 * @param timeUnit the {@link TimeUnit} to use.
	 * @return the {@link PollerSpec}
	 * @deprecated since 6.1 in favor of {@link #fixedDelay(Duration)}
	 * @see Pollers#fixedDelay(Duration)
	 */
	@Deprecated(forRemoval = true)
	public PollerSpec fixedDelay(long period, TimeUnit timeUnit) {
		return Pollers.fixedDelay(Duration.of(period, timeUnit.toChronoUnit()));
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and initial delay.
	 * @param period the fixed delay period (in milliseconds) to use.
	 * @param initialDelay the initial delay (in milliseconds) to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#fixedDelay(long, long)
	 */
	public PollerSpec fixedDelay(long period, long initialDelay) {
		return Pollers.fixedDelay(period, initialDelay);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period.
	 * @param period the fixed delay period to use.
	 * @return the {@link PollerSpec}
	 * @see Pollers#fixedDelay(long)
	 */
	public PollerSpec fixedDelay(long period) {
		return Pollers.fixedDelay(period);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period.
	 * @param period the fixed delay period to use.
	 * @return the {@link PollerSpec}
	 * @since 6.1
	 * @see Pollers#fixedDelay(Duration)
	 */
	public static PollerSpec fixedDelay(Duration period) {
		return Pollers.fixedDelay(period);
	}

	/**
	 * Create a {@link PollerSpec} based on the provided fixed delay period and initial delay .
	 * @param period the fixed delay period to use.
	 * @param initialDelay the initial delay to use.
	 * @return the {@link PollerSpec}
	 * @since 6.1
	 * @see Pollers#fixedDelay(Duration)
	 */
	public static PollerSpec fixedDelay(Duration period, Duration initialDelay) {
		return Pollers.fixedDelay(period, initialDelay);
	}

	PollerFactory() {
	}

}
