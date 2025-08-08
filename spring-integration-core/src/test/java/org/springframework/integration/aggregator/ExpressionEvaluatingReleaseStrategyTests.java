/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Peters
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class ExpressionEvaluatingReleaseStrategyTests {

	private ExpressionEvaluatingReleaseStrategy strategy;

	private final SimpleMessageGroup messages = new SimpleMessageGroup("foo");

	@Before
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setup() {
		for (int i = 0; i < 5; i++) {
			messages.add(new GenericMessage(i + 1));
		}
	}

	@Test
	public void testCompletedWithSizeSpelEvaluated() {
		strategy = new ExpressionEvaluatingReleaseStrategy("#root.size()==5");
		strategy.setBeanFactory(mock(BeanFactory.class));
		assertThat(strategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testCompletedWithFilterSpelEvaluated() {
		strategy = new ExpressionEvaluatingReleaseStrategy("!messages.?[payload==5].empty");
		strategy.setBeanFactory(mock(BeanFactory.class));
		assertThat(strategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testCompletedWithFilterSpelReturnsNotCompleted() {
		strategy = new ExpressionEvaluatingReleaseStrategy("!messages.?[payload==6].empty");
		strategy.setBeanFactory(mock(BeanFactory.class));
		assertThat(strategy.canRelease(messages)).isFalse();
	}

}
