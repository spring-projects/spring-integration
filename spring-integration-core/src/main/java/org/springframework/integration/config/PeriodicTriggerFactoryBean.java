/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
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

	public void setFixedDelayValue(String fixedDelayValue) {
		this.fixedDelayValue = fixedDelayValue;
	}

	public void setFixedRateValue(String fixedRateValue) {
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

		Duration duration = toDuration(hasFixedDelay ? this.fixedDelayValue : this.fixedRateValue, timeUnitToUse);

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
