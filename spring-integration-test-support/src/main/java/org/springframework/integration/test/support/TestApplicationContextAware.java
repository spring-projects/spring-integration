/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.support;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.integration.test.util.TestUtils;

/**
 * Utility interface for test classes that require access to a shared
 * {@link org.springframework.context.ApplicationContext} initialized via
 * {@link TestUtils#createTestApplicationContext()}.
 * <p>
 * This interface provides default {@code @BeforeAll} and {@code @AfterAll}
 * lifecycle hooks to initialize and close the test application context
 * once per test class.
 * <p>
 * It ensures that:
 * <ul>
 *   <li>The context is refreshed exactly once before any test runs.</li>
 *   <li>Any redundant refresh attempts are caught and suppressed (unless caused by an unrelated issue).</li>
 *   <li>The context is properly closed after all tests complete.</li>
 * </ul>
 * <p>
 * To use this interface, a test class simply needs to implement it.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.0
 */
public interface TestApplicationContextAware {

	TestUtils.TestApplicationContext TEST_INTEGRATION_CONTEXT = TestUtils.createTestApplicationContext();

	@BeforeAll
	@SuppressWarnings("NullAway")
	static void beforeAll() {
		try {
			TEST_INTEGRATION_CONTEXT.refresh();
		}
		catch (IllegalStateException ex) {
			if (!Objects.requireNonNull(ex.getMessage()).contains("just call 'refresh' once")) {
				throw ex;
			}
		}
		Object nullChannel = TEST_INTEGRATION_CONTEXT.getBean("nullChannel");
		TestUtils.getPropertyValue(nullChannel, "queue", LinkedBlockingQueue.class).clear();
	}

	@AfterAll
	static void tearDown() {
		TEST_INTEGRATION_CONTEXT.close();
	}

}
