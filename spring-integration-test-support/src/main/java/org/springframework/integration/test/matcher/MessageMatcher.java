/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Matcher to make assertions about message equality easier.  Usage:
 *
 * <pre class="code">
 * {@code
 * &#064;Test
 * public void testSomething() {
 *   Message<String> expected = ...;
 *   Message<String> result = ...;
 *   assertThat(result, sameExceptImmutableHeaders(expected));
 * }
 *
 * &#064;Factory
 * public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> expected) {
 *   return new MessageMatcher(expected);
 * }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 */
public class MessageMatcher extends BaseMatcher<Message<?>> {

	private final Object payload;

	private final Map<String, Object> headers;

	public MessageMatcher(Message<?> operand) {
		this.payload = operand.getPayload();
		this.headers = getHeaders(operand);
	}

	public boolean matches(Object arg) {
		Message<?> input = (Message<?>) arg;
		Map<String, Object> inputHeaders = getHeaders(input);
		return input.getPayload().equals(this.payload) && inputHeaders.equals(this.headers);
	}

	public void describeTo(Description description) {
		description.appendText("Headers match except ID and timestamp for payload: ")
				.appendValue(this.payload).appendText(" and headers: ")
				.appendValue(this.headers);
	}

	private static Map<String, Object> getHeaders(Message<?> operand) {
		HashMap<String, Object> headersToFilter = new HashMap<>(operand.getHeaders());
		headersToFilter.remove(MessageHeaders.ID);
		headersToFilter.remove(MessageHeaders.TIMESTAMP);
		return headersToFilter;
	}

}
