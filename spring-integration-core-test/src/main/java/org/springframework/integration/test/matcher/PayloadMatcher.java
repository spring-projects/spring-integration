/*
 * Copyright 2002-2014 the original author or authors.
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

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;

import org.springframework.messaging.Message;

/**
 * Is the payload of a {@link Message} equal to a given value or is matching
 * a given matcher?
 * <p>
 * A Junit example using {@link Assert#assertThat(Object, Matcher)} could look
 * like this to test a payload value:
 *
 * <pre class="code">
 * {@code
 * ANY_PAYLOAD = new BigDecimal("1.123");
 * Message<BigDecimal message = MessageBuilder.withPayload(ANY_PAYLOAD).build();
 * assertThat(message, hasPayload(ANY_PAYLOjAD));
 * }
 * </pre>
 *
 * <p>
 * An example using {@link Assert#assertThat(Object, Matcher)} delegating to
 * another {@link Matcher}.
 *
 * <pre class="code">
 * ANY_PAYLOAD = new BigDecimal("1.123");
 * assertThat(message, PayloadMatcher.hasPayload(is(BigDecimal.class)));
 * assertThat(message, PayloadMatcher.hasPayload(notNullValue()));
 * assertThat(message, not((PayloadMatcher.hasPayload(is(String.class))))); *
 * </pre>
 *
 *
 *
 * @author Alex Peters
 * @author Iwein Fuld
 *
 */
@SuppressWarnings("rawtypes")
public class PayloadMatcher extends TypeSafeMatcher<Message> {

	private final Matcher matcher;

	/**
	 * Create a PayloadMatcher that matches the payload of messages against the given matcher
	 */
	PayloadMatcher(Matcher matcher) {
		super();
		this.matcher = matcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean matchesSafely(Message message) {
		return matcher.matches(message.getPayload());
	}

	/**
	 * {@inheritDoc}
	 */
	//@Override
	@Override
	public void describeTo(Description description) {
		description.appendText("a Message with payload: ").appendDescriptionOf(matcher);

	}

	@Factory
	public static <T> Matcher<Message> hasPayload(T payload) {
		return new PayloadMatcher(IsEqual.equalTo(payload));
	}

	@Factory
	public static <T> Matcher<Message> hasPayload(Matcher<? super T> payloadMatcher) {
		return new PayloadMatcher(payloadMatcher);
	}
}
