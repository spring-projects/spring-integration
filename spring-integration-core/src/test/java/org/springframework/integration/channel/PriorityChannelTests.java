/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PriorityChannelTests {

	@Test
	public void testCapacityEnforced() {
		PriorityChannel channel = new PriorityChannel(3);
		assertTrue(channel.send(new StringMessage("test1"), 0));
		assertTrue(channel.send(new StringMessage("test2"), 0));
		assertTrue(channel.send(new StringMessage("test3"), 0));
		assertFalse(channel.send(new StringMessage("test4"), 0));
		channel.receive(0);
		assertTrue(channel.send(new StringMessage("test5")));
	}

	@Test
	public void testDefaultComparator() {
		PriorityChannel channel = new PriorityChannel(5);
		Message<?> priority1 = createPriorityMessage(1);
		Message<?> priority2 = createPriorityMessage(2);
		Message<?> priority3 = createPriorityMessage(3);
		Message<?> priority4 = createPriorityMessage(4);
		Message<?> priority5 = createPriorityMessage(5);
		channel.send(priority4);
		channel.send(priority3);
		channel.send(priority5);
		channel.send(priority1);
		channel.send(priority2);
		assertEquals("test-1", channel.receive(0).getPayload());
		assertEquals("test-2", channel.receive(0).getPayload());
		assertEquals("test-3", channel.receive(0).getPayload());
		assertEquals("test-4", channel.receive(0).getPayload());
		assertEquals("test-5", channel.receive(0).getPayload());
	}

	@Test
	public void testCustomComparator() {
		PriorityChannel channel = new PriorityChannel(5, null, new StringPayloadComparator());
		Message<?> messageA = new StringMessage("A");
		Message<?> messageB = new StringMessage("B");
		Message<?> messageC = new StringMessage("C");
		Message<?> messageD = new StringMessage("D");
		Message<?> messageE = new StringMessage("E");
		channel.send(messageC);
		channel.send(messageA);
		channel.send(messageE);
		channel.send(messageD);
		channel.send(messageB);
		assertEquals("A", channel.receive(0).getPayload());
		assertEquals("B", channel.receive(0).getPayload());
		assertEquals("C", channel.receive(0).getPayload());
		assertEquals("D", channel.receive(0).getPayload());
		assertEquals("E", channel.receive(0).getPayload());		
	}


	private static Message<?> createPriorityMessage(int priority) {
		Message<?> message = new StringMessage("test-" + priority);
		message.getHeader().setPriority(priority);
		return message;
	}


	public static class StringPayloadComparator implements Comparator<Message<?>> {

		public int compare(Message<?> message1, Message<?> message2) {
			String s1 = (String) message1.getPayload();
			String s2 = (String) message2.getPayload();
			return s1.compareTo(s2);
		}	
	}

}
