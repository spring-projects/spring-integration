/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.core;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class MessageHistoryTests {

	@Test
	public void addComponents() {
		GenericMessage<String> original = new GenericMessage<String>("foo");
		assertNull(MessageHistory.read(original));
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		MessageHistory history1 = MessageHistory.read(result1);
		assertNotNull(history1);
		assertEquals("testComponent-1", history1.toString());
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		MessageHistory history2 = MessageHistory.read(result2);
		assertNotNull(history2);
		assertEquals("testComponent-1,testComponent-2", history2.toString());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void verifyImmutability() {
		Message<?> message = MessageHistory.write(MessageBuilder.withPayload("test").build(), new TestComponent(1));
		MessageHistory history = MessageHistory.read(message);
		history.add(new Properties());
	}

	@Test
	public void testCorrectMutableMessageAfterWrite() {
		MutableMessage<String> original = new MutableMessage<>("foo");
		assertNull(MessageHistory.read(original));
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1, instanceOf(MutableMessage.class));
		assertSame(original, result1);
		MessageHistory history1 = MessageHistory.read(result1);
		assertNotNull(history1);
		assertEquals("testComponent-1", history1.toString());
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertSame(original, result2);
		MessageHistory history2 = MessageHistory.read(result2);
		assertNotNull(history2);
		assertEquals("testComponent-1,testComponent-2", history2.toString());
	}

	@Test
	public void testCorrectErrorMessageAfterWrite() {
		RuntimeException payload = new RuntimeException();
		ErrorMessage original = new ErrorMessage(payload);
		assertNull(MessageHistory.read(original));
		Message<Throwable> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1, instanceOf(ErrorMessage.class));
		assertNotSame(original, result1);
		assertSame(original.getPayload(), result1.getPayload());
		MessageHistory history1 = MessageHistory.read(result1);
		assertNotNull(history1);
		assertEquals("testComponent-1", history1.toString());
		Message<Throwable> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertThat(result2, instanceOf(ErrorMessage.class));
		assertNotSame(original, result2);
		assertNotSame(result1, result2);
		assertSame(original.getPayload(), result2.getPayload());
		MessageHistory history2 = MessageHistory.read(result2);
		assertNotNull(history2);
		assertEquals("testComponent-1,testComponent-2", history2.toString());
	}

	@Test
	public void testCorrectAdviceMessageAfterWrite() {
		Message<?> inputMessage = new GenericMessage<>("input");
		AdviceMessage<String> original = new AdviceMessage<>("foo", inputMessage);
		assertNull(MessageHistory.read(original));
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1, instanceOf(AdviceMessage.class));
		assertNotSame(original, result1);
		assertSame(original.getPayload(), result1.getPayload());
		assertSame(original.getInputMessage(), ((AdviceMessage<?>) result1).getInputMessage());
		MessageHistory history1 = MessageHistory.read(result1);
		assertNotNull(history1);
		assertEquals("testComponent-1", history1.toString());
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertThat(result2, instanceOf(AdviceMessage.class));
		assertNotSame(original, result2);
		assertSame(original.getPayload(), result2.getPayload());
		assertSame(original.getInputMessage(), ((AdviceMessage<?>) result2).getInputMessage());
		assertNotSame(result1, result2);
		MessageHistory history2 = MessageHistory.read(result2);
		assertNotNull(history2);
		assertEquals("testComponent-1,testComponent-2", history2.toString());
	}


	private static class TestComponent implements NamedComponent {

		private final int id;

		private TestComponent(int id) {
			this.id = id;
		}

		@Override
		public String getComponentName() {
			return "testComponent-" + this.id;
		}

		@Override
		public String getComponentType() {
			return "type-" + this.id;
		}
	}

}
