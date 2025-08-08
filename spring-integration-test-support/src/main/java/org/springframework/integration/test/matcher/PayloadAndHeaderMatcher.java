/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Matcher to make assertions about message equality easier. Usage:
 *
 * <pre>
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
 *   return new PayloadAndHeaderMatcher(expected);
 * }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
public final class PayloadAndHeaderMatcher<T> extends BaseMatcher<Message<?>> {

	private final T payload;

	private final Map<String, Object> headers;

	private final String[] ignoreKeys;

	public static <P> PayloadAndHeaderMatcher<P> sameExceptIgnorableHeaders(Message<P> expected, String... ignoreKeys) {
		return new PayloadAndHeaderMatcher<>(expected, ignoreKeys);
	}

	private PayloadAndHeaderMatcher(Message<T> expected, String... ignoreKeys) {
		this.ignoreKeys = ignoreKeys != null ? Arrays.copyOf(ignoreKeys, ignoreKeys.length) : null;
		this.payload = expected.getPayload();
		this.headers = extractHeadersToAssert(expected);
	}

	private Map<String, Object> extractHeadersToAssert(Message<?> operand) {
		HashMap<String, Object> headersToAssert = new HashMap<>(operand.getHeaders());
		headersToAssert.remove(MessageHeaders.ID);
		headersToAssert.remove(MessageHeaders.TIMESTAMP);
		if (this.ignoreKeys != null) {
			for (String key : this.ignoreKeys) {
				headersToAssert.remove(key);
			}
		}
		return headersToAssert;
	}

	@Override
	public boolean matches(Object arg) {
		Message<?> input = (Message<?>) arg;
		Map<String, Object> inputHeaders = extractHeadersToAssert(input);
		return input.getPayload().equals(this.payload) && inputHeaders.equals(this.headers);
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a Message with Headers that match except ID and timestamp for payload: ")
				.appendValue(this.payload)
				.appendText(" and headers: ")
				.appendValue(this.headers);
	}

}
