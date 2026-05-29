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
import org.junitpioneer.jupiter.RetryingTest;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

/**
 * @author Jiandong Ma
 */
class EnableIntegrationTests {

	/**
	 * A minimal application context containing only Spring Integration infrastructure
	 * beans (with no messaging annotations) is used to compare context startup time
	 * when the {@code processAnnotations} attribute in {@link EnableIntegration}
	 * is set to {@code true} versus {@code false}.
	 *
	 * <p> Uses {@link RetryingTest} in case of JVM pauses.
	 */
	@RetryingTest(maxAttempts = 10)
	void processAnnotationsPerformanceTest() {
		// JVM warmup
		for (int i = 0; i < 2; i++) {
			new AnnotationConfigApplicationContext(ConfigWithProcessAnnotations.class);
			new AnnotationConfigApplicationContext(ConfigWithoutProcessAnnotations.class);
		}

		StopWatch stopWatch = new StopWatch();

		stopWatch.start();
		new AnnotationConfigApplicationContext(ConfigWithProcessAnnotations.class);
		stopWatch.stop();
		long processAnnotationsTime = stopWatch.lastTaskInfo().getTimeMillis();

		stopWatch.start();
		new AnnotationConfigApplicationContext(ConfigWithoutProcessAnnotations.class);
		stopWatch.stop();
		long notProcessAnnotationsTime = stopWatch.lastTaskInfo().getTimeMillis();

		Assertions.assertThat(processAnnotationsTime).isGreaterThan(notProcessAnnotationsTime);
	}

	@Configuration
	@EnableIntegration(processAnnotations = true)
	static class ConfigWithProcessAnnotations {

	}

	@Configuration
	@EnableIntegration(processAnnotations = false)
	static class ConfigWithoutProcessAnnotations {

	}

}
