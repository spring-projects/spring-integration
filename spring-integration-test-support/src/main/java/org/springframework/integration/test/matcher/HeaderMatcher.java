/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import java.util.Date;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Are the {@link MessageHeaders} of a {@link Message} containing any entry
 * or multiple that match?
 * <p>
 * For example using {@link org.junit.Assert#assertThat(Object, Matcher)} for a single
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
 * @author Gary Russell
 *
 */
public final class HeaderMatcher<T> extends TypeSafeMatcher<Message<T>> {

	private final Matcher<?> matcher;

	/**
	 * @param matcher the target matcher to delegate
	 */
	private HeaderMatcher(Matcher<?> matcher) {
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

	public static <P, V> HeaderMatcher<P> hasHeader(String key, V value) {
		return new HeaderMatcher<>(Matchers.hasEntry(key, value));
	}

	public static <P, V> HeaderMatcher<P> hasHeader(String key, Matcher<V> valueMatcher) {
		return new HeaderMatcher<>(Matchers.hasEntry(Matchers.is(key), valueMatcher));
	}

	public static <P> HeaderMatcher<P> hasHeaderKey(String key) {
		return new HeaderMatcher<>(Matchers.hasKey(key));
	}

	public static <P> HeaderMatcher<P> hasAllHeaders(Map<String, ?> entries) {
		return new HeaderMatcher<>(MapContentMatchers.hasAllEntries(entries));
	}

	public static <P, V> HeaderMatcher<P> hasMessageId(V value) {
		return new HeaderMatcher<>(Matchers.hasEntry(MessageHeaders.ID, value));
	}

	public static <P, V> HeaderMatcher<P> hasCorrelationId(V value) {
		return new HeaderMatcher<>(Matchers.hasEntry("correlationId", value));
	}

	public static <P> HeaderMatcher<P> hasSequenceNumber(Integer value) {
		return hasSequenceNumber(Matchers.is(value));
	}

	public static <P> HeaderMatcher<P> hasSequenceNumber(Matcher<Integer> matcher) {
		return new HeaderMatcher<>(Matchers.hasEntry(Matchers.is("sequenceNumber"), matcher));
	}

	public static <P> HeaderMatcher<P> hasSequenceSize(Integer value) {
		return hasSequenceSize(Matchers.is(value));
	}

	public static <P> HeaderMatcher<P> hasSequenceSize(Matcher<Integer> value) {
		return new HeaderMatcher<>(Matchers.hasEntry(Matchers.is("sequenceSize"), value));
	}

	public static <P> HeaderMatcher<P> hasExpirationDate(Date value) {
		return hasExpirationDate(Matchers.is(value.getTime()));
	}

	public static <P> HeaderMatcher<P> hasExpirationDate(Matcher<Long> matcher) {
		return new HeaderMatcher<>(Matchers.hasEntry(Matchers.is("expirationDate"), matcher));
	}

	public static <P> HeaderMatcher<P> hasTimestamp(Date value) {
		return hasTimestamp(Matchers.is(value.getTime()));
	}

	public static <P> HeaderMatcher<P> hasTimestamp(Matcher<Long> matcher) {
		return new HeaderMatcher<>(Matchers.hasEntry(Matchers.is(MessageHeaders.TIMESTAMP), matcher));
	}

}
