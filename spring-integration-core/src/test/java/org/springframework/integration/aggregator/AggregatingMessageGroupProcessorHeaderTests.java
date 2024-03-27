/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class AggregatingMessageGroupProcessorHeaderTests {

	private final DefaultAggregatingMessageGroupProcessor defaultProcessor =
			new DefaultAggregatingMessageGroupProcessor();

	private final MethodInvokingMessageGroupProcessor methodInvokingProcessor =
			new MethodInvokingMessageGroupProcessor(new TestAggregatorBean(), "aggregate");

	@Before
	public void setup() {
		this.defaultProcessor.setBeanFactory(mock(BeanFactory.class));
		this.methodInvokingProcessor.setBeanFactory(mock(BeanFactory.class));
	}

	@Test
	public void singleMessageUsingDefaultProcessor() {
		singleMessage(this.defaultProcessor);
	}

	@Test
	public void singleMessageUsingMethodInvokingProcessor() {
		singleMessage(this.methodInvokingProcessor);
	}

	@Test
	public void twoMessagesWithoutConflictsUsingDefaultProcessor() {
		twoMessagesWithoutConflicts(this.defaultProcessor);
	}

	@Test
	public void twoMessagesWithoutConflictsUsingMethodInvokingProcessor() {
		twoMessagesWithoutConflicts(this.methodInvokingProcessor);
	}

	@Test
	public void twoMessagesWithConflictsUsingDefaultProcessor() {
		twoMessagesWithConflicts(this.defaultProcessor);
	}

	@Test
	public void twoMessagesWithConflictsUsingMethodInvokingProcessor() {
		twoMessagesWithConflicts(this.methodInvokingProcessor);
	}

	@Test
	public void missingValuesDoNotConflictUsingDefaultProcessor() {
		missingValuesDoNotConflict(this.defaultProcessor);
	}

	@Test
	public void missingValuesDoNotConflictUsingMethodInvokingProcessor() {
		missingValuesDoNotConflict(this.methodInvokingProcessor);
	}

	@Test
	public void multipleValuesConflictUsingDefaultProcessor() {
		multipleValuesConflict(this.defaultProcessor);
	}

	@Test
	public void multipleValuesConflictUsingMethodInvokingProcessor() {
		multipleValuesConflict(this.methodInvokingProcessor);
	}

	@Test
	public void testNullHeaderValue() {
		DefaultAggregatingMessageGroupProcessor processor = new DefaultAggregatingMessageGroupProcessor();
		DirectFieldAccessor dfa = new DirectFieldAccessor(processor);
		dfa.setPropertyValue("messageBuilderFactory", new MutableMessageBuilderFactory());
		Map<String, Object> headers1 = new HashMap<>();
		headers1.put("k1", "foo");
		headers1.put("k2", null);
		Message<?> message1 = MutableMessageBuilder.withPayload("test")
				.setCorrelationId(1)
				.setSequenceNumber(1)
				.setSequenceSize(2)
				.copyHeadersIfAbsent(headers1)
				.build();
		Map<String, Object> headers2 = new HashMap<>();
		headers2.put("k1", "bar");
		headers2.put("k2", 123);
		Message<?> message2 = correlatedMessage(1, 2, 2, headers2);
		List<Message<?>> messages = Arrays.asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("k1")).isNull();
		assertThat(resultMessage.getHeaders().get("k2")).isNull();

		headers1 = new HashMap<>();
		headers1.put("k1", "foo");
		headers1.put("k2", 123);
		message1 = correlatedMessage(1, 2, 1, headers1);
		headers2 = new HashMap<>();
		headers2.put("k1", "bar");
		headers2.put("k2", null);
		message2 = MutableMessageBuilder.withPayload("test")
				.setCorrelationId(1)
				.setSequenceNumber(2)
				.setSequenceSize(2)
				.copyHeadersIfAbsent(headers2)
				.build();
		messages = Arrays.asList(message1, message2);
		group = new SimpleMessageGroup(messages, 1);
		result = processor.processMessageGroup(group);
		resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("k1")).isNull();
		assertThat(resultMessage.getHeaders().get("k2")).isNull();
	}

	private void singleMessage(MessageGroupProcessor processor) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("k1", "value1");
		headers.put("k2", 2);
		Message<?> message = correlatedMessage(1, 1, 1, headers);
		List<Message<?>> messages = Collections.singletonList(message);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("k1")).isEqualTo("value1");
		assertThat(resultMessage.getHeaders().get("k2")).isEqualTo(2);
	}

	private void twoMessagesWithoutConflicts(MessageGroupProcessor processor) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("k1", "value1");
		headers.put("k2", 2);
		Message<?> message1 = correlatedMessage(1, 2, 1, headers);
		Message<?> message2 = correlatedMessage(1, 2, 2, headers);
		List<Message<?>> messages = Arrays.asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("k1")).isEqualTo("value1");
		assertThat(resultMessage.getHeaders().get("k2")).isEqualTo(2);
	}

	private void twoMessagesWithConflicts(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<>();
		headers1.put("k1", "foo");
		headers1.put("k2", 123);
		Message<?> message1 = correlatedMessage(1, 2, 1, headers1);
		Map<String, Object> headers2 = new HashMap<>();
		headers2.put("k1", "bar");
		headers2.put("k2", 123);
		Message<?> message2 = correlatedMessage(1, 2, 2, headers2);
		List<Message<?>> messages = Arrays.asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("k1")).isNull();
		assertThat(resultMessage.getHeaders().get("k2")).isEqualTo(123);
	}

	private void missingValuesDoNotConflict(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<>();
		headers1.put("only1", "value1");
		headers1.put("commonTo1And2", "foo");
		headers1.put("commonToAll", 123);
		headers1.put("conflictBetween1And2", "valueFor1");
		Message<?> message1 = correlatedMessage(1, 3, 1, headers1);
		Map<String, Object> headers2 = new HashMap<>();
		headers2.put("only2", "value2");
		headers2.put("commonTo1And2", "foo");
		headers2.put("commonTo2And3", "bar");
		headers2.put("conflictBetween1And2", "valueFor2");
		headers2.put("conflictBetween2And3", "valueFor2");
		headers2.put("commonToAll", 123);
		Message<?> message2 = correlatedMessage(1, 3, 2, headers2);
		Map<String, Object> headers3 = new HashMap<>();
		headers3.put("only3", "value3");
		headers3.put("commonTo2And3", "bar");
		headers3.put("commonToAll", 123);
		headers3.put("conflictBetween2And3", "valueFor3");
		Message<?> message3 = correlatedMessage(1, 3, 3, headers3);
		List<Message<?>> messages = Arrays.asList(message1, message2, message3);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("only1")).isEqualTo("value1");
		assertThat(resultMessage.getHeaders().get("only2")).isEqualTo("value2");
		assertThat(resultMessage.getHeaders().get("only3")).isEqualTo("value3");
		assertThat(resultMessage.getHeaders().get("commonTo1And2")).isEqualTo("foo");
		assertThat(resultMessage.getHeaders().get("commonTo2And3")).isEqualTo("bar");
		assertThat(resultMessage.getHeaders().get("commonToAll")).isEqualTo(123);
		assertThat(resultMessage.getHeaders().get("conflictBetween1And2")).isNull();
		assertThat(resultMessage.getHeaders().get("conflictBetween2And3")).isNull();
	}

	private void multipleValuesConflict(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<>();
		headers1.put("common", "valueForAll");
		headers1.put("conflict", "valueFor1");
		Message<?> message1 = correlatedMessage(1, 3, 1, headers1);
		Map<String, Object> headers2 = new HashMap<>();
		headers2.put("common", "valueForAll");
		headers2.put("conflict", "valueFor2");
		Message<?> message2 = correlatedMessage(1, 3, 2, headers2);
		Map<String, Object> headers3 = new HashMap<>();
		headers3.put("conflict", "valueFor3");
		headers3.put("common", "valueForAll");
		Message<?> message3 = correlatedMessage(1, 3, 3, headers3);
		List<Message<?>> messages = Arrays.asList(message1, message2, message3);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertThat(result).isNotNull();
		assertThat(result instanceof AbstractIntegrationMessageBuilder<?>).isTrue();
		Message<?> resultMessage = ((AbstractIntegrationMessageBuilder<?>) result).build();
		assertThat(resultMessage.getHeaders().get("common")).isEqualTo("valueForAll");
		assertThat(resultMessage.getHeaders().get("conflict")).isNull();
	}

	private static Message<?> correlatedMessage(Object correlationId, Integer sequenceSize,
			Integer sequenceNumber, Map<String, Object> headers) {

		return MessageBuilder.withPayload("test")
				.setCorrelationId(correlationId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.copyHeadersIfAbsent(headers)
				.build();
	}

	private static class TestAggregatorBean {

		TestAggregatorBean() {
			super();
		}

		@SuppressWarnings("unused")
		public Object aggregate(List<String> payloads) {
			StringBuilder sb = new StringBuilder();
			for (String s : payloads) {
				sb.append(s);
			}
			return sb.toString();
		}

	}

}
