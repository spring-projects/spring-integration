/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alex Peters
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ExpressionEvaluatingMessageGroupProcessorTests implements TestApplicationContextAware {

	private ExpressionEvaluatingMessageGroupProcessor processor;

	private final MessageGroup group = mock();

	private final List<Message<?>> messages = new ArrayList<>();

	@BeforeEach
	public void setup() {
		messages.clear();
		for (int i = 0; i < 5; i++) {
			messages.add(MessageBuilder.withPayload(i + 1).setHeader("foo", "bar").build());
		}
	}

	@Test
	public void testProcessAndSendWithSizeExpressionEvaluated() {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root.size()");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object result = processor.processMessageGroup(group);
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getPayload()).isEqualTo(5);
	}

	@Test
	public void testProcessAndCheckHeaders() {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("#root");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object result = processor.processMessageGroup(group);
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void testProcessAndSendWithProjectionExpressionEvaluated() {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("![payload]");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object result = processor.processMessageGroup(group);
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getPayload() instanceof Collection<?>).isTrue();
		Collection<?> list = (Collection<?>) resultMessage.getPayload();
		assertThat(list.size()).isEqualTo(5);
		assertThat(list.contains(1)).isTrue();
		assertThat(list.contains(2)).isTrue();
		assertThat(list.contains(3)).isTrue();
		assertThat(list.contains(4)).isTrue();
		assertThat(list.contains(5)).isTrue();
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionExpressionEvaluated() {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor("?[payload>2].![payload]");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object result = processor.processMessageGroup(group);
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getPayload() instanceof Collection<?>).isTrue();
		Collection<?> list = (Collection<?>) resultMessage.getPayload();
		assertThat(list.size()).isEqualTo(3);
		assertThat(list.contains(3)).isTrue();
		assertThat(list.contains(4)).isTrue();
		assertThat(list.contains(5)).isTrue();
	}

	@Test
	public void testProcessAndSendWithFilterAndProjectionAndMethodInvokingExpressionEvaluated() {
		when(group.getMessages()).thenReturn(messages);
		processor = new ExpressionEvaluatingMessageGroupProcessor(String.format("T(%s).sum(?[payload>2].![payload])",
				getClass().getName()));
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object result = processor.processMessageGroup(group);
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getPayload()).isEqualTo(3 + 4 + 5);
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
