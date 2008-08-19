/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Date;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class MessageBuilderTests {

	@Test
	public void testSimpleMessageCreation() {
		Message<String> message = MessageBuilder.fromPayload("foo").build();
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testHeaderValues() {
		Message<String> message = MessageBuilder.fromPayload("test")
				.setHeader("foo", "bar")
				.setHeader("count", new Integer(123))
				.build();
		assertEquals("bar", message.getHeaders().get("foo", String.class));
		assertEquals(new Integer(123), message.getHeaders().get("count", Integer.class));
	}

	@Test
	public void testCopiedHeaderValues() {
		Message<String> message1 = MessageBuilder.fromPayload("test1")
				.setHeader("foo", "1")
				.setHeader("bar", "2")
				.build();
		Message<String> message2 = MessageBuilder.fromPayload("test2")
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

	@Test
	public void copyHeadersIfAbsent() {
		Message<String> message1 = MessageBuilder.fromPayload("test1")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromPayload("test2")
				.setHeader("foo", 123)
				.copyHeadersIfAbsent(message1.getHeaders())
				.build();
		assertEquals("test2", message2.getPayload());
		assertEquals(123, message2.getHeaders().get("foo"));
	}

	@Test
	public void createFromMessage() {
		Message<String> message1 = MessageBuilder.fromPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertEquals("test", message2.getPayload());
		assertEquals("bar", message2.getHeaders().get("foo"));
	}

	@Test
	public void testPriority() {
		Message<Integer> importantMessage = MessageBuilder.fromPayload(1)
			.setPriority(MessagePriority.HIGHEST).build();
		assertEquals(MessagePriority.HIGHEST, importantMessage.getHeaders().getPriority());
	}

	@Test
	public void testNonDestructiveSet() {
		Message<Integer> message1 = MessageBuilder.fromPayload(1)
			.setPriority(MessagePriority.HIGHEST).build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.setHeaderIfAbsent(MessageHeaders.PRIORITY, MessagePriority.LOW)
			.build();
		assertEquals(MessagePriority.HIGHEST, message2.getHeaders().getPriority());
	}

	@Test
	public void testExpirationDate() {
		Date past = new Date(System.currentTimeMillis() - (60 * 1000));
		Message<Integer> expiredMessage = MessageBuilder.fromPayload(1)
				.setExpirationDate(past).build();
		assertEquals(past, expiredMessage.getHeaders().getExpirationDate()); 
	}

}
