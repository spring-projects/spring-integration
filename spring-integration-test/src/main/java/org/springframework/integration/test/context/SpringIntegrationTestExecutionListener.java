/*
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.test.context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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

	private Set<AbstractEndpoint> autoStartupCandidates = new HashSet<>();

	@Override
	public void prepareTestInstance(TestContext testContext) {
		SpringIntegrationTest springIntegrationTest =
				TestContextAnnotationUtils.findMergedAnnotation(testContext.getTestClass(), SpringIntegrationTest.class);

		String[] patterns = springIntegrationTest != null ? springIntegrationTest.noAutoStartup() : new String[0];

		ApplicationContext applicationContext = testContext.getApplicationContext();
		MockIntegrationContext mockIntegrationContext = applicationContext.getBean(MockIntegrationContext.class);
		this.autoStartupCandidates = mockIntegrationContext.getAutoStartupCandidates();
		this.autoStartupCandidates.stream()
				.filter(endpoint -> !match(endpoint.getBeanName(), patterns))
				.peek(endpoint -> endpoint.setAutoStartup(true))
				.forEach(AbstractEndpoint::start);
	}

	@Override
	public void afterTestClass(TestContext testContext) {
		this.autoStartupCandidates.stream()
				.peek(endpoint -> endpoint.setAutoStartup(false))
				.forEach(AbstractEndpoint::stop);
	}

	private static boolean match(@Nullable String name, String[] patterns) {
		return patterns.length > 0 &&
				Arrays.stream(patterns)
						.anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, name));
	}

}
