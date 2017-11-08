/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Date;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Are the {@link MessageHeaders} of a {@link Message} containing any entry
 * or multiple that match?
 * <p>
 * For example using {@link Assert#assertThat(Object, Matcher)} for a single
 * entry:
 *
 * <pre class="code">
 * {@code
 * ANY_HEADER_KEY = "foo";
 * ANY_HEADER_VALUE = "bar";
 * assertThat(message, hasEntry(ANY_HEADER_KEY, ANY_HEADER_VALUE));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, is(String.class)));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, notNullValue()));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, is(ANY_HEADER_VALUE)));
 * }
 * </pre>
 * <p>
 * For multiple entries to match all:
 * <pre class="code">
 * {@code
 * Map<String, Object> expectedInHeaderMap = new HashMap<String, Object>();
 * expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE);
 * expectedInHeaderMap.put(OTHER_HEADER_KEY, is(OTHER_HEADER_VALUE));
 * assertThat(message, HeaderMatcher.hasAllEntries(expectedInHeaderMap));
 * }
 * </pre>
 *
 * <p>
 * For a single key:
 *
 * <pre class="code">
 * ANY_HEADER_KEY = "foo";
 * assertThat(message, HeaderMatcher.hasKey(ANY_HEADER_KEY));
 * </pre>
 *
 *
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Artem Bilan
 *
 */
public class HeaderMatcher<T> extends TypeSafeMatcher<Message<T>> {

	private final Matcher<?> matcher;

	/**
	 * @param matcher the target matcher to delegate
	 */
	private HeaderMatcher(Matcher<?> matcher) {
		super();
		this.matcher = matcher;
	}

	@Override
	public boolean matchesSafely(Message<T> item) {
		return this.matcher.matches(item.getHeaders());
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a Message with Headers containing ")
				.appendDescriptionOf(this.matcher);
	}

	@Factory
	public static <P, V> HeaderMatcher<P> hasHeader(String key, V value) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry(key, value));
	}

	@Factory
	public static <P, V> HeaderMatcher<P> hasHeader(String key, Matcher<V> valueMatcher) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry(key, valueMatcher));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasHeaderKey(String key) {
		return new HeaderMatcher<>(MapContentMatchers.hasKey(key));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasAllHeaders(Map<String, ?> entries) {
		return new HeaderMatcher<>(MapContentMatchers.hasAllEntries(entries));
	}

	@Factory
	public static <P, V> HeaderMatcher<P> hasMessageId(V value) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry(MessageHeaders.ID, value));
	}

	@Factory
	public static <P, V> HeaderMatcher<P> hasCorrelationId(V value) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry("correlationId", value));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasSequenceNumber(Integer value) {
		return hasSequenceNumber(CoreMatchers.is(value));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasSequenceNumber(Matcher<Integer> matcher) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry("sequenceNumber", matcher));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasSequenceSize(Integer value) {
		return hasSequenceSize(CoreMatchers.is(value));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasSequenceSize(Matcher<Integer> value) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry("sequenceSize", value));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasExpirationDate(Date value) {
		return hasExpirationDate(CoreMatchers.is(value.getTime()));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasExpirationDate(Matcher<Long> matcher) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry("expirationDate", matcher));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasTimestamp(Date value) {
		return hasTimestamp(CoreMatchers.is(value.getTime()));
	}

	@Factory
	public static <P> HeaderMatcher<P> hasTimestamp(Matcher<Long> matcher) {
		return new HeaderMatcher<>(MapContentMatchers.hasEntry(MessageHeaders.TIMESTAMP, matcher));
	}

}
