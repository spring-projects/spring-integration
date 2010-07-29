/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;

/**
 * @author Alex Peters
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionEvaluatingMessageGroupProcessorTests {

	private ExpressionEvaluatingMessageGroupProcessor processor;
	
	private MessagingTemplate template = new MessagingTemplate();

	@Mock
	private MessageChannel outputChannel;

	@Mock
	private MessageGroup group;

	List<Message<?>> messages = new ArrayList<Message<?>>();

	@Before
	public void setup() {
		when(outputChannel.send(isA(Message.class))).thenReturn(true);
		messages.clear();
		for (int i = 0; i < 5; i++) {
			messages.add(MessageBuilder.withPayload(i + 1).setHeader("foo", "bar").build());
		}
	}

	@Test
	public void testProcessAndSendWithSizeExpressionEvaluated() throws Exception {
		when(group.getUnmarked()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root.size()");
		processor.processAndSend(group, template, outputChannel);
		verify(outputChannel).send(messageWithPayload(5));
	}

	@Test
	public void testProcessAndCheckHeaders() throws Exception {
		when(group.getUnmarked()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root");
		processor.processAndSend(group, template, outputChannel);
		verify(outputChannel).send(messageWithHeader("foo", "bar"));
	}

	@Test
	public void testProcessAndSendWithProjectionExpressionEvaluated() throws Exception {
		when(group.getUnmarked()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("![payload]");
		processor.processAndSend(group, template, outputChannel);
		verify(outputChannel).send(messageWithPayload(hasItems(1, 2, 3, 4, 5)));
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionExpressionEvaluated() throws Exception {
		when(group.getUnmarked()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("?[payload>2].![payload]");
		processor.processAndSend(group, template, outputChannel);
		verify(outputChannel).send(messageWithPayload(hasItems(3, 4, 5)));
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionAndMethodInvokingExpressionEvaluated() throws Exception {
		when(group.getUnmarked()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor(String.format("T(%s).sum(?[payload>2].![payload])",
				getClass().getName()));
		processor.processAndSend(group, template, outputChannel);
		verify(outputChannel).send(messageWithPayload(3 + 4 + 5));
	}

	private Message<?> messageWithHeader(String key, Object value) {
		return Matchers.argThat(MessageMatcher.hasHeader(IsMapContaining.hasEntry(key, value)));
	}

	private Message<?> messageWithPayload(Matcher<?> matcher) {
		return Matchers.argThat(MessageMatcher.hasPayload(matcher));
	}

	private Message<?> messageWithPayload(int i) {
		return Matchers.argThat(MessageMatcher.hasPayload(IsEqual.equalTo(i)));
	}

	/*
	 * sample static method invoked by SpEL
	 */
	public static Integer sum(Collection<Integer> values) {
		int result = 0;
		for (Integer value : values) {
			result += value;
		}
		return result;
	}
	
	private static class MessageMatcher extends TypeSafeMatcher<Message<?>> {

		private final Matcher<?> payloadMatcher;

		private final Matcher<?> headerMatcher;

		/**
		 * @param matcher
		 */
		MessageMatcher(Matcher<?> matcher, Matcher<?> headerMatcher) {
			super();
			this.payloadMatcher = matcher;
			this.headerMatcher = headerMatcher;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean matchesSafely(Message<?> message) {
			return payloadMatcher.matches(message.getPayload()) && headerMatcher.matches(message.getHeaders());
		}

		/**
		 * {@inheritDoc}
		 */
		//@Override
		public void describeTo(Description description) {
			description.appendText("a Message with payload: ").appendDescriptionOf(payloadMatcher);
			description.appendText(" and headers: ").appendDescriptionOf(headerMatcher);
		}

		@Factory
		public static <T> Matcher<Message<?>> hasPayload(Matcher<T> payloadMatcher) {
			return new MessageMatcher(payloadMatcher, CoreMatchers.anything());
		}

		@Factory
		public static <T> Matcher<Message<?>> hasHeader(Matcher<T> headerMatcher) {
			return new MessageMatcher(CoreMatchers.anything(), headerMatcher);
		}
	}

}
