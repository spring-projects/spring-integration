/*
 * Copyright 2026-present the original author or authors.
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StopWatch;

/**
 * @author Jiandong Ma
 */
class EnableIntegrationTests {

	@Test
	void parseAnnotationsPerformanceTest() {
		long startupTimeWhenParseAnnotationsEnabled;
		long startupTimeWhenParseAnnotationsDisabled;
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
			AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			reader.register(ConfigWithParseAnnotationsEnabled.class);
			context.refresh();

			stopWatch.stop();
			startupTimeWhenParseAnnotationsEnabled = stopWatch.getTotalTimeMillis();
		}
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
			AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();

			reader.register(ConfigWithParseAnnotationsDisabled.class);
			context.refresh();

			stopWatch.stop();
			startupTimeWhenParseAnnotationsDisabled = stopWatch.getTotalTimeMillis();
		}
		// with this minimal configuration, startup time is already greatly improved.
		// @EnableIntegration - takes nearly 900ms
		// @EnableIntegration(parseAnnotations = false) - takes nearly 30ms
		Assertions.assertThat(startupTimeWhenParseAnnotationsEnabled)
				.isGreaterThan(startupTimeWhenParseAnnotationsDisabled);
	}

	@Configuration
	@EnableIntegration
	static class ConfigWithParseAnnotationsEnabled {

	}

	@Configuration
	@EnableIntegration(parseAnnotations = false)
	static class ConfigWithParseAnnotationsDisabled {

	}

}
