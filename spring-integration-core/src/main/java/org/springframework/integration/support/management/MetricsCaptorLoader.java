/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.support.management;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptor;
import org.springframework.util.ClassUtils;

/**
 * Iterates over a map of classes to check for presence (key) and classes to try to load
 * (value) (if the key is on the classpath). The class to load must be a
 * {@link MetricsCaptor} and have a static method {@code loadCaptor(ApplicationContext)}.
 * User code can add to or replace the default {@link MicrometerMetricsCaptor}.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
public final class MetricsCaptorLoader {

	private static final Map<String, String> captors = new LinkedHashMap<>(
			Collections.singletonMap("io.micrometer.core.instrument.MeterRegistry",
					"org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptor"));

	private MetricsCaptorLoader() {
		super();
	}

	/**
	 * Set captors to check.
	 * @param newCaptors The captors (class to check for presence : class to load). Should be a
	 * {@link LinkedHashMap} so that the desired order is maintained.
	 * @param replace true to replace the default captor(s).
	 */
	public static void setCaptors(Map<String, String> newCaptors, boolean replace) {
		if (replace) {
			captors.clear();
		}
		captors.putAll(newCaptors);
	}

	/**
	 * Load the first captor into the application context where the class represented
	 * by the key is present and the {@link MetricsCaptor} represented by the captors.value
	 * is present.
	 * @param applicationContext the context.
	 * @return the captor.
	 */
	public static MetricsCaptor loadCaptor(ApplicationContext applicationContext) {
		ClassLoader classLoader = MetricsCaptorLoader.class.getClassLoader();
		for (Entry<String, String> entry : captors.entrySet()) {
			try {
				if (ClassUtils.isPresent(entry.getKey(), classLoader)) {
					Class<?> captor = ClassUtils.forName(entry.getValue(), classLoader);
					if (MetricsCaptor.class.isAssignableFrom(captor)) {
						Method method = captor.getDeclaredMethod("loadCaptor", ApplicationContext.class);
						return (MetricsCaptor) method.invoke(null, applicationContext);
					}
				}
			}
			catch (Exception e) {
				// no op
			}
		}
		return null;
	}

}
