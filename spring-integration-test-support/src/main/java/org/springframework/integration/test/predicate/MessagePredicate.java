/*
 * Copyright 2019 the original author or authors.
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
