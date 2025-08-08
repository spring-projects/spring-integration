/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.test.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Predicate to make assertions about message equality easier.  Usage:
 *
 * <pre class="code">
 * {@code
 * &#064;Test
 * public void testSomething() {
 *   Message<String> expected = ...;
 *   Message<String> result = ...;
 *   assertThat(result).matches(new MessagePredicate(expected));
 * }
 * }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class MessagePredicate implements Predicate<Message<?>> {

	private final Object payload;

	private final Map<String, Object> headers;

	private final List<String> ignoredHeaders =
			new ArrayList<>(Arrays.asList(MessageHeaders.ID, MessageHeaders.TIMESTAMP));

	public MessagePredicate(Message<?> operand, String... ignoredHeaders) {
		this.payload = operand.getPayload();
		if (ignoredHeaders != null) {
			this.ignoredHeaders.addAll(Arrays.asList(ignoredHeaders));
		}
		this.headers = getHeaders(operand);
	}

	@Override
	public boolean test(Message<?> input) {
		Map<String, Object> inputHeaders = getHeaders(input);
		return input.getPayload().equals(this.payload) && inputHeaders.equals(this.headers);
	}

	private Map<String, Object> getHeaders(Message<?> operand) {
		HashMap<String, Object> headersToFilter = new HashMap<>(operand.getHeaders());
		this.ignoredHeaders.forEach(headersToFilter::remove);
		return headersToFilter;
	}

}
