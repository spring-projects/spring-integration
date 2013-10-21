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

package org.springframework.integration.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
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


	private static class TestComponent implements NamedComponent {

		private final int id;

		private TestComponent(int id) {
			this.id = id;
		}

		public String getComponentName() {
			return "testComponent-" + this.id;
		}

		public String getComponentType() {
			return "type-" + this.id;
		}
	}

}
