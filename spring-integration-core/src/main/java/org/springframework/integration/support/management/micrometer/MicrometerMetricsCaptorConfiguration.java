/*
 * Copyright 2020-2022 the original author or authors.
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
