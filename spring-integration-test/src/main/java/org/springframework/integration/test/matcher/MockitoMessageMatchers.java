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

import static org.mockito.Matchers.argThat;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.util.Map;

import org.hamcrest.Matcher;
import org.mockito.Mockito;

import org.springframework.messaging.Message;

/**
 * Mockito matcher factory for {@link Message} matcher creation.
 * <p>
 * This class contains expressive factory methods for the most common Mockito
 * matchers needed when matching {@link Message}s. If you need a different
 * matching strategy, any Hamcrest matcher can be used in Mockito through
 * {@link Mockito#argThat(Matcher)}.
 *
 * Example usage:
 * <p>
 * With {@link Mockito#verify(Object)}:
 * </p>
 *
 * <pre class="code">
 * {@code
 * &#064;Mock
 * MessageHandler handler;
 * ...
 * handler.handleMessage(message);
 * verify(handler).handleMessage(messageWithPayload(SOME_PAYLOAD));
 * verify(handler).handleMessage(messageWithPayload(is(SOME_CLASS)));
 * }
 * </pre>
 * <p>
 * With {@link Mockito#when(Object)}:
 * </p>
 *
 * <pre class="code">
 * {@code
 * ...
 * when(channel.send(messageWithPayload(SOME_PAYLOAD))).thenReturn(true);
 * assertThat(channel.send(message), is(true));
 * }
 * </pre>
 *
 * @author Alex Peters
 * @author Iwein Fuld
 *
 */
public class MockitoMessageMatchers {

	@SuppressWarnings("unchecked")
	public static <T> Message<T> messageWithPayload(Matcher<T> payloadMatcher) {
		return argThat(hasPayload(payloadMatcher));
	}

	@SuppressWarnings("unchecked")
	public static <T> Message<T> messageWithPayload(T payload) {
		return argThat(hasPayload(payload));
	}

	public static Message<?> messageWithHeaderEntry(String key, Object value) {
		return argThat(hasHeader(key, value));
	}

	public static Message<?> messageWithHeaderKey(String key) {
		return argThat(HeaderMatcher.hasHeaderKey(key));
	}

	public static <T> Message<?> messageWithHeaderEntry(String key,
			Matcher<T> valueMatcher) {
		return argThat(HeaderMatcher.<T> hasHeader(key, valueMatcher));
	}

	public static Message<?> messageWithHeaderEntries(Map<String, ?> entries) {
		return argThat(HeaderMatcher.hasAllHeaders(entries));
	}
}
