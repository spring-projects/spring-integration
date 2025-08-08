/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.matcher;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.exceptions.verification.opentest4j.ArgumentsAreDifferent;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MockitoMessageMatchersTests {

	static final Date SOME_PAYLOAD = new Date();

	static final String SOME_HEADER_VALUE = "bar";

	static final String SOME_HEADER_KEY = "test.foo";

	@Mock
	MessageHandler handler;

	@Mock
	MessageChannel channel;

	Message<Date> message;

	@Before
	public void setUp() {
		this.message =
				MessageBuilder.withPayload(SOME_PAYLOAD)
						.setHeader(SOME_HEADER_KEY, SOME_HEADER_VALUE)
						.build();
	}

	@Test
	public void anyMatcher_withVerifyArgumentMatcherAndEqualPayload_matching() {
		this.handler.handleMessage(this.message);
		verify(this.handler).handleMessage(MockitoMessageMatchers.messageWithPayload(SOME_PAYLOAD));
		verify(this.handler)
				.handleMessage(MockitoMessageMatchers.messageWithPayload(Matchers.is(Matchers.instanceOf(Date.class))));
	}

	@Test
	public void anyMatcher_withVerifyAndDifferentPayload_notMatching() {
		this.handler.handleMessage(this.message);
		assertThatExceptionOfType(ArgumentsAreDifferent.class)
				.isThrownBy(() ->
						verify(this.handler)
								.handleMessage(MockitoMessageMatchers.messageWithPayload(Matchers.nullValue())));
	}

	@Test
	public void anyMatcher_withWhenArgumentMatcherAndEqualPayload_matching() {
		when(this.channel.send(MockitoMessageMatchers.messageWithPayload(SOME_PAYLOAD))).thenReturn(true);
		assertThat(channel.send(this.message)).isTrue();
	}

	@Test
	public void anyMatcher_withWhenAndDifferentPayload_notMatching() {
		when(this.channel.send(
				MockitoMessageMatchers.messageWithHeaderEntry(SOME_HEADER_KEY,
						Matchers.is(Matchers.instanceOf(Short.class)))))
				.thenReturn(true);
		assertThat(this.channel.send(this.message)).isFalse();
	}

}
