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

package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Alex Peters
 * @author Mark Fisher
 * @author Gary Russell
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionEvaluatingMessageGroupProcessorTests {

	private ExpressionEvaluatingMessageGroupProcessor processor;

	@Mock
	private MessageGroup group;

	private final List<Message<?>> messages = new ArrayList<Message<?>>();


	@Before
	public void setup() {
		messages.clear();
		for (int i = 0; i < 5; i++) {
			messages.add(MessageBuilder.withPayload(i + 1).setHeader("foo", "bar").build());
		}
	}


	@Test
	public void testProcessAndSendWithSizeExpressionEvaluated() throws Exception {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root.size()");
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessageGroup(group);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals(5, resultMessage.getPayload());
	}

	@Test
	public void testProcessAndCheckHeaders() throws Exception {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root");
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessageGroup(group);
		processor.setBeanFactory(mock(BeanFactory.class));
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals("bar", resultMessage.getHeaders().get("foo"));
	}

	@Test
	public void testProcessAndSendWithProjectionExpressionEvaluated() throws Exception {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("![payload]");
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessageGroup(group);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertTrue(resultMessage.getPayload() instanceof Collection<?>);
		Collection<?> list = (Collection<?>) resultMessage.getPayload();
		assertEquals(5, list.size());
		assertTrue(list.contains(1));
		assertTrue(list.contains(2));
		assertTrue(list.contains(3));
		assertTrue(list.contains(4));
		assertTrue(list.contains(5));
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionExpressionEvaluated() throws Exception {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("?[payload>2].![payload]");
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessageGroup(group);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertTrue(resultMessage.getPayload() instanceof Collection<?>);
		Collection<?> list = (Collection<?>) resultMessage.getPayload();
		assertEquals(3, list.size());
		assertTrue(list.contains(3));
		assertTrue(list.contains(4));
		assertTrue(list.contains(5));
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionAndMethodInvokingExpressionEvaluated() throws Exception {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor(String.format("T(%s).sum(?[payload>2].![payload])",
				getClass().getName()));
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessageGroup(group);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals(3 + 4 + 5, resultMessage.getPayload());
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

}
