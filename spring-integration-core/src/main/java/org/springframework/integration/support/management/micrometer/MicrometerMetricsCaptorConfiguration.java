/*
 * Copyright 2020-2021 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.util.ClassUtils;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * An {@link ImportBeanDefinitionRegistrar} to conditionally add a {@link MicrometerMetricsCaptor}
 * bean when {@code io.micrometer.core.instrument.MeterRegistry} is present in classpath and
 * no {@link MicrometerMetricsCaptor#MICROMETER_CAPTOR_NAME} bean present yet.
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
	public MicrometerMetricsCaptor micrometerMetricsCaptor(ObjectProvider<MeterRegistry> meterRegistries) {
		if (meterRegistries.stream().findAny().isPresent()) {
			return new MicrometerMetricsCaptor(meterRegistries);
		}
		else {
			return null;
		}
	}

}
