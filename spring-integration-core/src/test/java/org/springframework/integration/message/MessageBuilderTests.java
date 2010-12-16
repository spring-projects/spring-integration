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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class MessageBuilderTests {

	@Test
	public void testSimpleMessageCreation() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testHeaderValues() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar")
				.setHeader("count", new Integer(123))
				.build();
		assertEquals("bar", message.getHeaders().get("foo", String.class));
		assertEquals(new Integer(123), message.getHeaders().get("count", Integer.class));
	}

	@Test
	public void testCopiedHeaderValues() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "1")
				.setHeader("bar", "2")
				.build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.copyHeaders(message1.getHeaders())
				.setHeader("foo", "42")
				.setHeaderIfAbsent("bar", "99")
				.build();
		assertEquals("test1", message1.getPayload());
		assertEquals("test2", message2.getPayload());
		assertEquals("1", message1.getHeaders().get("foo"));
		assertEquals("42", message2.getHeaders().get("foo"));
		assertEquals("2", message1.getHeaders().get("bar"));
		assertEquals("2", message2.getHeaders().get("bar"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdHeaderValueReadOnly() {
		UUID id = UUID.randomUUID();
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.ID, id);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTimestampValueReadOnly() {
		Long timestamp = 12345L;
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.TIMESTAMP, timestamp).build();
	}

	@Test
	public void copyHeadersIfAbsent() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setHeader("foo", 123)
				.copyHeadersIfAbsent(message1.getHeaders())
				.build();
		assertEquals("test2", message2.getPayload());
		assertEquals(123, message2.getHeaders().get("foo"));
	}

	@Test
	public void createFromMessage() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertEquals("test", message2.getPayload());
		assertEquals("bar", message2.getHeaders().get("foo"));
	}

	@Test
	public void createIdRegenerated() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertEquals("bar", message2.getHeaders().get("foo"));
		assertNotSame(message1.getHeaders().getId(), message2.getHeaders().getId());
	}

	@Test
	public void testPriority() {
		Message<Integer> importantMessage = MessageBuilder.withPayload(1)
			.setPriority(123).build();
		assertEquals(new Integer(123), importantMessage.getHeaders().getPriority());
	}

	@Test
	public void testNonDestructiveSet() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
			.setPriority(42).build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.setHeaderIfAbsent(MessageHeaders.PRIORITY, 13)
			.build();
		assertEquals(new Integer(42), message2.getHeaders().getPriority());
	}

	@Test
	public void testExpirationDateSetAsLong() {
		Long past = System.currentTimeMillis() - (60 * 1000);
		Message<Integer> expiredMessage = MessageBuilder.withPayload(1)
				.setExpirationDate(past).build();
		assertEquals(past, expiredMessage.getHeaders().getExpirationDate()); 
	}

	@Test
	public void testExpirationDateSetAsDate() {
		Long past = System.currentTimeMillis() - (60 * 1000);
		Message<Integer> expiredMessage = MessageBuilder.withPayload(1)
				.setExpirationDate(new Date(past)).build();
		assertEquals(past, expiredMessage.getHeaders().getExpirationDate()); 
	}

	@Test
	public void testRemove() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
			.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.removeHeader("foo")
			.build();
		assertFalse(message2.getHeaders().containsKey("foo"));
	}

	@Test
	public void testSettingToNullRemoves() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
			.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.setHeader("foo", null)
			.build();
		assertFalse(message2.getHeaders().containsKey("foo"));
	}
	
	@Test
	public void testPushAndPopSequenceDetails() throws Exception {
		Message<Integer> message1 = MessageBuilder.withPayload(1).pushSequenceDetails("foo", 1, 2).build();
		assertFalse(message1.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertTrue(message2.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertFalse(message3.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoCorrelationId() throws Exception {
		Message<Integer> message1 = MessageBuilder.withPayload(1).build();
		assertFalse(message1.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertFalse(message2.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertFalse(message3.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
	}

	@Test
	public void testPopSequenceDetailsWhenNotPopped() throws Exception {
		Message<Integer> message1 = MessageBuilder.withPayload(1).build();
		assertFalse(message1.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).popSequenceDetails().build();
		assertFalse(message2.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoSequence() throws Exception {
		Message<Integer> message1 = MessageBuilder.withPayload(1).setCorrelationId("foo").build();
		assertFalse(message1.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertTrue(message2.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertFalse(message3.getHeaders().containsKey(MessageHeaders.SEQUENCE_DETAILS));
	}

}
