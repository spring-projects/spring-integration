/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * An {@link ImportSelector} to conditionally load a {@link MicrometerMetricsCaptorConfiguration}
 * when {@code io.micrometer.core.instrument.MeterRegistry} is present in classpath.
 *
 * @author Artem Bilan
 *
 * @since 5.2.9
 */
public class MicrometerImportSelector implements DeferredImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		if (ClassUtils.isPresent("io.micrometer.core.instrument.MeterRegistry", ClassUtils.getDefaultClassLoader())) {
			return new String[]{ MicrometerMetricsCaptorConfiguration.class.getName() };
		}
		return new String[0];
	}

}
