/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
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
 *
 */
public class PayloadAndHeaderMatcher extends BaseMatcher<Message<?>> {

	private final Object payload;

	private final Map<String, Object> headers;

	private final String[] ignoreKeys;

	@Factory
	public static Matcher<Message<?>> sameExceptIgnorableHeaders(Message<?> expected, String... ignoreKeys) {
		return new PayloadAndHeaderMatcher(expected, ignoreKeys);
	}

	private PayloadAndHeaderMatcher(Message<?> expected, String... ignoreKeys) {
		this.ignoreKeys = ignoreKeys;
		this.payload = expected.getPayload();
		this.headers = getHeaders(expected);
	}

	private Map<String, Object> getHeaders(Message<?> operand) {
		HashMap<String, Object> headers = new HashMap<String, Object>(operand.getHeaders());
		headers.remove(MessageHeaders.ID);
		headers.remove(MessageHeaders.TIMESTAMP);
		for (String key : ignoreKeys) {
			headers.remove(key);
		}
		return headers;
	}

	public boolean matches(Object arg) {
		Message<?> input = (Message<?>) arg;
		Map<String, Object> inputHeaders = getHeaders(input);
		return input.getPayload().equals(payload) && inputHeaders.equals(headers);
	}

	public void describeTo(Description description) {
		description.appendText("a Message with Headers that match except ID and timestamp for payload: ").appendValue(
				payload).appendText(" and headers: ").appendValue(headers);
	}

}
