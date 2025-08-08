/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
