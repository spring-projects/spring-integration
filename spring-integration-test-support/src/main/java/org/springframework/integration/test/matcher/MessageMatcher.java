/*
 * Copyright 2016-2019 the original author or authors.
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
