/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

import org.springframework.messaging.Message;

/**
 * Is the payload of a {@link Message} equal to a given value or is matching
 * a given matcher?
 * <p>
 * A Junit example using {@link org.junit.Assert#assertThat(Object, Matcher)} could look
 * like this to test a payload value:
 *
 * <pre class="code">
 * {@code
 * ANY_PAYLOAD = new BigDecimal("1.123");
 * Message<BigDecimal> message = MessageBuilder.withPayload(ANY_PAYLOAD).build();
 * assertThat(message, hasPayload(ANY_PAYLOAD));
 * }
 * </pre>
 *
 * <p>
 * An example using {@link org.junit.Assert#assertThat(Object, Matcher)} delegating to
 * another {@link Matcher}.
 *
 * <pre class="code">
 * {@code
 * ANY_PAYLOAD = new BigDecimal("1.123");
 * Message<BigDecimal> message = MessageBuilder.withPayload(ANY_PAYLOAD).build();
 * assertThat(message, PayloadMatcher.hasPayload(is(BigDecimal.class)));
 * assertThat(message, PayloadMatcher.hasPayload(notNullValue()));
 * assertThat(message, not((PayloadMatcher.hasPayload(is(String.class))))); *
 * }
 * </pre>
 *
 *
 *
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
public final class PayloadMatcher<T> extends TypeSafeMatcher<Message<?>> {

	private final Matcher<T> matcher;

	/**
	 * Create a PayloadMatcher that matches the payload of messages against the given matcher
	 */
	private PayloadMatcher(Matcher<T> matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean matchesSafely(Message<?> message) {
		return this.matcher.matches(message.getPayload());
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a Message with payload: ")
				.appendDescriptionOf(this.matcher);

	}

	public static <P> PayloadMatcher<P> hasPayload(P payload) {
		return new PayloadMatcher<>(IsEqual.equalTo(payload));
	}

	public static <P> PayloadMatcher<P> hasPayload(Matcher<P> payloadMatcher) {
		return new PayloadMatcher<>(payloadMatcher);
	}

}
