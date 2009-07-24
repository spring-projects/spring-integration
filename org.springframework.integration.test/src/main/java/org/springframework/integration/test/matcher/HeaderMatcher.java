/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;

/**
 * <h2>Are the {@link MessageHeaders} of a {@link Message} containing any entry
 * or multiple that match?</h2>
 * 
 * 
 * <h3>
 * For example using {@link Assert#assertThat(Object, Matcher)} for a single
 * entry:</h3>
 * 
 * <pre>
 * ANY_HEADER_KEY = &quot;foo&quot;;
 * ANY_HEADER_VALUE = &quot;bar&quot;;
 * assertThat(message, hasEntry(ANY_HEADER_KEY, ANY_HEADER_VALUE));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, is(String.class)));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, notNullValue()));
 * assertThat(message, hasEntry(ANY_HEADER_KEY, is(ANY_HEADER_VALUE)));
 * </pre>
 * 
 * <h3>For multiple entries to match all:</h3>
 * 
 * <pre>
 * Map&lt;String, Object&gt; expectedInHeaderMap = new HashMap&lt;String, Object&gt;();
 * expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE);
 * expectedInHeaderMap.put(OTHER_HEADER_KEY, is(OTHER_HEADER_VALUE));
 * assertThat(message, HeaderMatcher.hasAllEntries(expectedInHeaderMap));
 * </pre>
 * 
 * <h3>
 * For a single key:</h3>
 * 
 * <pre>
 * ANY_HEADER_KEY = &quot;foo&quot;;
 * assertThat(message, HeaderMatcher.hasKey(ANY_HEADER_KEY));
 * </pre>
 * 
 * 
 * @author Alex Peters
 * @author Iwein Fuld
 * 
 */
public class HeaderMatcher extends TypeSafeMatcher<Message<?>> {

	private final Matcher<?> matcher;

	/**
	 * @param matcher
	 */
	HeaderMatcher(Matcher<?> matcher) {
		super();
		this.matcher = matcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean matchesSafely(Message<?> item) {
		return matcher.matches(item.getHeaders());
	}

	/**
	 * {@inheritDoc}
	 */
	public void describeTo(Description description) {
		description.appendText("a Message with Headers containing ").appendDescriptionOf(matcher);

	}

	@Factory
	public static <T> Matcher<Message<?>> hasHeader(String key, T value) {
		return new HeaderMatcher(MapContentMatchers.hasEntry(key, value));
	}

	@Factory
	public static <T> Matcher<Message<?>> hasHeader(String key, Matcher<?> valueMatcher) {
		return new HeaderMatcher(MapContentMatchers.hasEntry(key, valueMatcher));
	}

	@Factory
	public static <T> Matcher<Message<?>> hasHeaderKey(String key) {
		return new HeaderMatcher(MapContentMatchers.hasKey(key));
	}

	@Factory
	public static Matcher<Message<?>> hasAllHeaders(Map<String, ?> entries) {
		return new HeaderMatcher(MapContentMatchers.hasAllEntries(entries));
	}
}