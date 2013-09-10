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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class AggregatingMessageGroupProcessorHeaderTests {

	private final DefaultAggregatingMessageGroupProcessor defaultProcessor = new DefaultAggregatingMessageGroupProcessor();

	private final MethodInvokingMessageGroupProcessor methodInvokingProcessor =
			new MethodInvokingMessageGroupProcessor(new TestAggregatorBean(), "aggregate");

	@Test
	public void singleMessageUsingDefaultProcessor() {
		this.singleMessage(defaultProcessor);
	}

	@Test
	public void singleMessageUsingMethodInvokingProcessor() {
		this.singleMessage(methodInvokingProcessor);
	}

	@Test
	public void twoMessagesWithoutConflictsUsingDefaultProcessor() {
		this.twoMessagesWithoutConflicts(defaultProcessor);
	}

	@Test
	public void twoMessagesWithoutConflictsUsingMethodInvokingProcessor() {
		this.twoMessagesWithoutConflicts(methodInvokingProcessor);
	}

	@Test
	public void twoMessagesWithConflictsUsingDefaultProcessor() {
		this.twoMessagesWithConflicts(defaultProcessor);
	}

	@Test
	public void twoMessagesWithConflictsUsingMethodInvokingProcessor() {
		this.twoMessagesWithConflicts(methodInvokingProcessor);
	}

	@Test
	public void missingValuesDoNotConflictUsingDefaultProcessor() {
		this.missingValuesDoNotConflict(defaultProcessor);
	}

	@Test
	public void missingValuesDoNotConflictUsingMethodInvokingProcessor() {
		this.missingValuesDoNotConflict(methodInvokingProcessor);
	}

	@Test
	public void multipleValuesConflictUsingDefaultProcessor() {
		this.multipleValuesConflict(defaultProcessor);
	}

	@Test
	public void multipleValuesConflictUsingMethodInvokingProcessor() {
		this.multipleValuesConflict(methodInvokingProcessor);
	}


	private void singleMessage(MessageGroupProcessor processor) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("k1", "value1");
		headers.put("k2", new Integer(2));
		Message<?> message = correlatedMessage(1, 1, 1, headers);
		List<Message<?>> messages = Collections.<Message<?>>singletonList(message);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals("value1", resultMessage.getHeaders().get("k1"));
		assertEquals(2, resultMessage.getHeaders().get("k2"));
	}

	private void twoMessagesWithoutConflicts(MessageGroupProcessor processor) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("k1", "value1");
		headers.put("k2", new Integer(2));
		Message<?> message1 = correlatedMessage(1, 2, 1, headers);
		Message<?> message2 = correlatedMessage(1, 2, 2, headers);
		List<Message<?>> messages = Arrays.<Message<?>>asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals("value1", resultMessage.getHeaders().get("k1"));
		assertEquals(2, resultMessage.getHeaders().get("k2"));
	}

	private void twoMessagesWithConflicts(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		headers1.put("k1", "foo");
		headers1.put("k2", new Integer(123));
		Message<?> message1 = correlatedMessage(1, 2, 1, headers1);
		Map<String, Object> headers2 = new HashMap<String, Object>();
		headers2.put("k1", "bar");
		headers2.put("k2", new Integer(123));		
		Message<?> message2 = correlatedMessage(1, 2, 2, headers2);
		List<Message<?>> messages = Arrays.<Message<?>>asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertNull(resultMessage.getHeaders().get("k1"));
		assertEquals(123, resultMessage.getHeaders().get("k2"));
	}

	private void missingValuesDoNotConflict(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		headers1.put("only1", "value1");
		headers1.put("commonTo1And2", "foo");
		headers1.put("commonToAll", new Integer(123));
		headers1.put("conflictBetween1And2", "valueFor1");
		Message<?> message1 = correlatedMessage(1, 3, 1, headers1);
		Map<String, Object> headers2 = new HashMap<String, Object>();
		headers2.put("only2", "value2");
		headers2.put("commonTo1And2", "foo");
		headers2.put("commonTo2And3", "bar");
		headers2.put("conflictBetween1And2", "valueFor2");
		headers2.put("conflictBetween2And3", "valueFor2");
		headers2.put("commonToAll", new Integer(123));		
		Message<?> message2 = correlatedMessage(1, 3, 2, headers2);
		Map<String, Object> headers3 = new HashMap<String, Object>();
		headers3.put("only3", "value3");
		headers3.put("commonTo2And3", "bar");
		headers3.put("commonToAll", new Integer(123));
		headers3.put("conflictBetween2And3", "valueFor3");
		Message<?> message3 = correlatedMessage(1, 3, 3, headers3);
		List<Message<?>> messages = Arrays.<Message<?>>asList(message1, message2, message3);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals("value1", resultMessage.getHeaders().get("only1"));
		assertEquals("value2", resultMessage.getHeaders().get("only2"));
		assertEquals("value3", resultMessage.getHeaders().get("only3"));
		assertEquals("foo", resultMessage.getHeaders().get("commonTo1And2"));
		assertEquals("bar", resultMessage.getHeaders().get("commonTo2And3"));
		assertEquals(123, resultMessage.getHeaders().get("commonToAll"));
		assertNull(resultMessage.getHeaders().get("conflictBetween1And2"));
		assertNull(resultMessage.getHeaders().get("conflictBetween2And3"));
	}

	private void multipleValuesConflict(MessageGroupProcessor processor) {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		headers1.put("common", "valueForAll");
		headers1.put("conflict", "valueFor1");
		Message<?> message1 = correlatedMessage(1, 3, 1, headers1);
		Map<String, Object> headers2 = new HashMap<String, Object>();
		headers2.put("common", "valueForAll");
		headers2.put("conflict", "valueFor2");
		Message<?> message2 = correlatedMessage(1, 3, 2, headers2);
		Map<String, Object> headers3 = new HashMap<String, Object>();
		headers3.put("conflict", "valueFor3");
		headers3.put("common", "valueForAll");
		Message<?> message3 = correlatedMessage(1, 3, 3, headers3);
		List<Message<?>> messages = Arrays.<Message<?>>asList(message1, message2, message3);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertEquals("valueForAll", resultMessage.getHeaders().get("common"));
		assertNull(resultMessage.getHeaders().get("conflict"));
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
