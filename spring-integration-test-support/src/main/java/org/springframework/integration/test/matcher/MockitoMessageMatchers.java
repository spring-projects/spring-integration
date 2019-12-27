/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Map;

import org.hamcrest.Matcher;
import org.mockito.ArgumentMatchers;
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher;

import org.springframework.messaging.Message;

/**
 * Mockito matcher factory for {@link Message} matcher creation.
 * <p>
 * This class contains expressive factory methods for the most common Mockito
 * matchers needed when matching {@link Message}s. If you need a different
 * matching strategy, any Hamcrest matcher can be used in Mockito through
 * {@link org.mockito.Mockito#argThat(org.mockito.ArgumentMatcher)}.
 *
 * Example usage:
 * <p>
 * With {@link org.mockito.Mockito#verify(Object)}:
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
 * With {@link org.mockito.Mockito#when(Object)}:
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
 * @author Artem Bilan
 *
 */
public final class MockitoMessageMatchers {

	private MockitoMessageMatchers() {
	}

	public static <T> Message<?> messageWithPayload(Matcher<? super T> payloadMatcher) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(PayloadMatcher.hasPayload(payloadMatcher)));
	}

	public static <T> Message<?> messageWithPayload(T payload) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(PayloadMatcher.hasPayload(payload)));
	}

	public static Message<?> messageWithHeaderEntry(String key, Object value) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(HeaderMatcher.hasHeader(key, value)));
	}

	public static Message<?> messageWithHeaderKey(String key) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(HeaderMatcher.hasHeaderKey(key)));
	}

	public static <T> Message<?> messageWithHeaderEntry(String key, Matcher<T> valueMatcher) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(HeaderMatcher.hasHeader(key, valueMatcher)));
	}

	public static Message<?> messageWithHeaderEntries(Map<String, ?> entries) {
		return ArgumentMatchers.argThat(new HamcrestArgumentMatcher<>(HeaderMatcher.hasAllHeaders(entries)));
	}

}
