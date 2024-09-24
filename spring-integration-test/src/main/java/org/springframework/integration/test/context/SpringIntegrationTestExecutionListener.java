/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.integration.test.context;

import java.util.Arrays;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.PatternMatchUtils;

/**
 * A {@link TestExecutionListener} to customize {@link AbstractEndpoint} beans according
 * to the provided options in the {@link SpringIntegrationTest} annotation
 * on prepare test instance and after test class phases.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class SpringIntegrationTestExecutionListener implements TestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) {
		SpringIntegrationTest springIntegrationTest =
				TestContextAnnotationUtils.findMergedAnnotation(testContext.getTestClass(), SpringIntegrationTest.class);

		String[] patterns = springIntegrationTest != null ? springIntegrationTest.noAutoStartup() : new String[0];

		ApplicationContext applicationContext = testContext.getApplicationContext();
		MockIntegrationContext mockIntegrationContext = applicationContext.getBean(MockIntegrationContext.class);
		mockIntegrationContext.getAutoStartupCandidates()
				.stream()
				.filter(endpoint -> !match(endpoint.getBeanName(), patterns))
				.peek(endpoint -> endpoint.setAutoStartup(true))
				.forEach(AbstractEndpoint::start);
	}

	@Override
	public void afterTestClass(TestContext testContext) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		MockIntegrationContext mockIntegrationContext = applicationContext.getBean(MockIntegrationContext.class);
		mockIntegrationContext.getAutoStartupCandidates()
				.forEach(AbstractEndpoint::stop);
	}

	private boolean match(String name, String[] patterns) {
		return patterns.length > 0 &&
				Arrays.stream(patterns)
						.anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, name));
	}

}
