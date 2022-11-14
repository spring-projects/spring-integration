/*
 * Copyright 2002-2022 the original author or authors.
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
