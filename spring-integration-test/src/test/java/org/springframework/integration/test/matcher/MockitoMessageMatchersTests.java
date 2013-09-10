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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.integration.test.matcher.MockitoMessageMatchers.messageWithHeaderEntry;
import static org.springframework.integration.test.matcher.MockitoMessageMatchers.messageWithPayload;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MockitoMessageMatchersTests {

	static final Date SOME_PAYLOAD = new Date();

	static final String UNKNOWN_KEY = "unknownKey";

	static final String SOME_HEADER_VALUE = "bar";

	static final String SOME_HEADER_KEY = "test.foo";

	@Mock
	MessageHandler handler;

	@Mock
	MessageChannel channel;

	Message<Date> message;

	@Before
	public void setUp() {
		message = MessageBuilder.withPayload(SOME_PAYLOAD).setHeader(SOME_HEADER_KEY,
				SOME_HEADER_VALUE).build();
	}

	@Test
	public void anyMatcher_withVerifyArgumentMatcherAndEqualPayload_matching() throws Exception {
		handler.handleMessage(message);
		verify(handler).handleMessage(messageWithPayload(SOME_PAYLOAD));
		verify(handler).handleMessage(messageWithPayload(is(instanceOf(Date.class))));
	}

	@Test(expected = ArgumentsAreDifferent.class)
	public void anyMatcher_withVerifyAndDifferentPayload_notMatching() throws Exception {
		handler.handleMessage(message);
		verify(handler).handleMessage(messageWithPayload(nullValue()));
	}

	@Test
	public void anyMatcher_withWhenArgumentMatcherAndEqualPayload_matching() throws Exception {
		when(channel.send(messageWithPayload(SOME_PAYLOAD))).thenReturn(true);
		assertThat(channel.send(message), is(true));
	}

	@Test
	public void anyMatcher_withWhenAndDifferentPayload_notMatching() throws Exception {
		when(channel.send(messageWithHeaderEntry(SOME_HEADER_KEY, is(instanceOf(Short.class))))).thenReturn(true);
		assertThat(channel.send(message), is(false));
	}

}
