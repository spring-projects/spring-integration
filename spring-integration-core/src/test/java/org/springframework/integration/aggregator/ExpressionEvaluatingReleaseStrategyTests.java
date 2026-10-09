/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Peters
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class ExpressionEvaluatingReleaseStrategyTests implements TestApplicationContextAware {

	private ExpressionEvaluatingReleaseStrategy strategy;

	private final SimpleMessageGroup messages = new SimpleMessageGroup("foo");

	@BeforeEach
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setup() {
		for (int i = 0; i < 5; i++) {
			messages.add(new GenericMessage(i + 1));
		}
	}

	@Test
	public void testCompletedWithSizeSpelEvaluated() {
		strategy = new ExpressionEvaluatingReleaseStrategy("#root.size()==5");
		strategy.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(strategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testCompletedWithFilterSpelEvaluated() {
		strategy = new ExpressionEvaluatingReleaseStrategy("!messages.?[payload==5].empty");
		strategy.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(strategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testCompletedWithFilterSpelReturnsNotCompleted() {
		strategy = new ExpressionEvaluatingReleaseStrategy("!messages.?[payload==6].empty");
		strategy.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(strategy.canRelease(messages)).isFalse();
	}

}
