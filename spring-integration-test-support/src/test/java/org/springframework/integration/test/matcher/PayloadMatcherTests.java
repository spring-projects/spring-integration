/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import java.math.BigDecimal;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class PayloadMatcherTests {

	private static final BigDecimal ANY_PAYLOAD = new BigDecimal("1.123");

	private final Message<BigDecimal> message = MessageBuilder.withPayload(ANY_PAYLOAD).build();

	@Test
	public void hasPayload_withEqualValue_matches() {
		MatcherAssert.assertThat(this.message, PayloadMatcher.hasPayload(new BigDecimal("1.123")));
	}

	@Test
	public void hasPayload_withNotEqualValue_notMatching() {
		MatcherAssert.assertThat(this.message, Matchers.not(PayloadMatcher.hasPayload(new BigDecimal("456"))));
	}

	@Test
	public void hasPayload_withMatcher_matches() {
		MatcherAssert.assertThat(this.message,
				PayloadMatcher.hasPayload(Matchers.is(Matchers.instanceOf(BigDecimal.class))));
		MatcherAssert.assertThat(this.message, PayloadMatcher.hasPayload(Matchers.notNullValue()));
	}

	@Test
	public void hasPayload_withNotMatchingMatcher_notMatching() {
		MatcherAssert.assertThat(this.message,
				Matchers.not((PayloadMatcher.hasPayload(Matchers.is(Matchers.instanceOf(String.class))))));
	}

	@Test
	public void readableException() {
		try {
			MatcherAssert.assertThat(this.message, PayloadMatcher.hasPayload("woot"));
		}
		catch (AssertionError ae) {
			MatcherAssert.assertThat(ae.getMessage(), Matchers.containsString("Expected: a Message with payload: "));
		}
	}

}
