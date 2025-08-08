/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
