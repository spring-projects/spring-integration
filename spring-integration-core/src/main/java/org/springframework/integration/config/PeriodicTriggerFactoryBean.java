/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.integration.config;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link FactoryBean} to produce a {@link PeriodicTrigger}
 * based on parsing string values for its options.
 * This class is mostly driven by the XML configuration requirements for
 * {@link Duration} value representations for the respective attributes.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class PeriodicTriggerFactoryBean implements FactoryBean<PeriodicTrigger> {

	@Nullable
	private String fixedDelayValue;

	@Nullable
	private String fixedRateValue;

	@Nullable
	private String initialDelayValue;

	@Nullable
	private TimeUnit timeUnit;

	public void setFixedDelayValue(@Nullable String fixedDelayValue) {
		this.fixedDelayValue = fixedDelayValue;
	}

	public void setFixedRateValue(@Nullable String fixedRateValue) {
		this.fixedRateValue = fixedRateValue;
	}

	public void setInitialDelayValue(String initialDelayValue) {
		this.initialDelayValue = initialDelayValue;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	@Override
	public PeriodicTrigger getObject() {
		boolean hasFixedDelay = StringUtils.hasText(this.fixedDelayValue);
		boolean hasFixedRate = StringUtils.hasText(this.fixedRateValue);

		Assert.isTrue(hasFixedDelay ^ hasFixedRate,
				"One of the 'fixedDelayValue' or 'fixedRateValue' property must be provided but not both.");

		TimeUnit timeUnitToUse = this.timeUnit;
		if (timeUnitToUse == null) {
			timeUnitToUse = TimeUnit.MILLISECONDS;
		}

		String value = hasFixedDelay ? this.fixedDelayValue : this.fixedRateValue;
		Duration duration = toDuration(Objects.requireNonNull(value), timeUnitToUse);

		PeriodicTrigger periodicTrigger = new PeriodicTrigger(duration);
		periodicTrigger.setFixedRate(hasFixedRate);
		if (StringUtils.hasText(this.initialDelayValue)) {
			periodicTrigger.setInitialDelay(toDuration(this.initialDelayValue, timeUnitToUse));
		}
		return periodicTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return PeriodicTrigger.class;
	}

	private static Duration toDuration(String value, TimeUnit timeUnit) {
		if (isDurationString(value)) {
			return Duration.parse(value);
		}
		return toDuration(Long.parseLong(value), timeUnit);
	}

	private static boolean isDurationString(String value) {
		return (value.length() > 1 && (isP(value.charAt(0)) || isP(value.charAt(1))));
	}

	private static boolean isP(char ch) {
		return (ch == 'P' || ch == 'p');
	}

	private static Duration toDuration(long value, TimeUnit timeUnit) {
		return Duration.of(value, timeUnit.toChronoUnit());
	}

}
