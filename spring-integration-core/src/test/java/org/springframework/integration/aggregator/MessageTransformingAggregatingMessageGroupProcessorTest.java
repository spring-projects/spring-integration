/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Alen Turkovic
 * @since 5.1
 */
public class MessageTransformingAggregatingMessageGroupProcessorTest {

	private final MessageTransformingAggregatingMessageGroupProcessor processor = new MessageTransformingAggregatingMessageGroupProcessor();

	@Test
	public void singleMessage() {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("k1", "value1");
		headers.put("k2", 2);
		Message<?> message = correlatedMessage(1, 1, 1, headers);
		List<Message<?>> messages = Collections.<Message<?>>singletonList(message);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertTrue(resultMessage.getPayload() instanceof List);
		assertEquals(1, ((List) resultMessage.getPayload()).size());
		final Message<?> payloadMessage = (Message) ((List) resultMessage.getPayload()).get(0);
		assertEquals("test", payloadMessage.getPayload());
		assertEquals("value1", payloadMessage.getHeaders().get("k1"));
		assertEquals(2, payloadMessage.getHeaders().get("k2"));
		assertNull(resultMessage.getHeaders().get("k1"));
		assertNull(resultMessage.getHeaders().get("k2"));
	}

	@Test
	public void twoMessages() {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		headers1.put("k1", "foo");
		headers1.put("k2", 123);
		Message<?> message1 = correlatedMessage(1, 2, 1, headers1);
		Map<String, Object> headers2 = new HashMap<String, Object>();
		headers2.put("k1", "bar");
		headers2.put("k2", 123);
		Message<?> message2 = correlatedMessage(1, 2, 2, headers2);
		List<Message<?>> messages = Arrays.<Message<?>>asList(message1, message2);
		MessageGroup group = new SimpleMessageGroup(messages, 1);
		Object result = processor.processMessageGroup(group);
		assertNotNull(result);
		assertTrue(result instanceof Message<?>);
		Message<?> resultMessage = (Message<?>) result;
		assertTrue(resultMessage.getPayload() instanceof List);
		assertEquals(2, ((List) resultMessage.getPayload()).size());
		final Message<?> firstMessage = (Message) ((List) resultMessage.getPayload()).get(0);
		final Message<?> secondMessage = (Message) ((List) resultMessage.getPayload()).get(1);
		assertEquals("test", firstMessage.getPayload());
		assertEquals("foo", firstMessage.getHeaders().get("k1"));
		assertEquals(123, firstMessage.getHeaders().get("k2"));
		assertEquals("test", secondMessage.getPayload());
		assertEquals("bar", secondMessage.getHeaders().get("k1"));
		assertEquals(123, secondMessage.getHeaders().get("k2"));
		assertNull(resultMessage.getHeaders().get("k1"));
		assertNull(resultMessage.getHeaders().get("k2"));
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

}
