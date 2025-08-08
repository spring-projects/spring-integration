/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.management.micrometer;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.util.ClassUtils;

/**
 * A {@link Configuration} to add a {@link MicrometerMetricsCaptor}.
 *
 * @author Artem Bilan
 *
 * @since 5.2.9
 */
@Configuration(proxyBeanMethods = false)
public class MicrometerMetricsCaptorConfiguration {

	/**
	 * A {@code boolean} flag to indicate if the
	 * {@code io.micrometer.core.instrument.MeterRegistry} class is present in the
	 * CLASSPATH to allow a {@link MicrometerMetricsCaptor} bean.
	 */
	public static final boolean METER_REGISTRY_PRESENT =
			ClassUtils.isPresent("io.micrometer.core.instrument.MeterRegistry", null);

	@Bean(name = MicrometerMetricsCaptor.MICROMETER_CAPTOR_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public MicrometerMetricsCaptor micrometerMetricsCaptor(ObjectProvider<MeterRegistry> meterRegistries) {
		if (meterRegistries.stream().findAny().isPresent()) {
			return new MicrometerMetricsCaptor(meterRegistries);
		}
		else {
			return null;
		}
	}

}
