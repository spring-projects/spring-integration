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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class SimpleMessageStoreTests {

	@Test
	public void testPut() {
		SimpleMessageStore store = new SimpleMessageStore(5);
		Message<?> message1 = new StringMessage("message-1");
		Message<?> previous = store.put(1, message1);
		assertNull(previous);
		assertNotNull(store.get(1));
		assertEquals(message1, store.get(1));
	}

	@Test
	public void testReplace() {
		SimpleMessageStore store = new SimpleMessageStore(5);
		Message<?> messageA = new StringMessage("message-a");
		Message<?> messageB = new StringMessage("message-b");
		store.put(1, messageA);
		Message<?> previous = store.put(1, messageB);
		assertEquals(messageA, previous);
		assertEquals(messageB, store.get(1));
	}

	@Test
	public void testRemove() {
		SimpleMessageStore store = new SimpleMessageStore(5);
		Message<?> message = new StringMessage("message");
		assertNull(store.remove(1));
		store.put(1, message);
		assertEquals(message, store.remove(1));
	}

	@Test
	public void testCapacityEnforced() {
		SimpleMessageStore store = new SimpleMessageStore(3);
		Message<?> message1 = new StringMessage("message-1");
		Message<?> message2 = new StringMessage("message-2");
		Message<?> message3 = new StringMessage("message-3");
		Message<?> message4 = new StringMessage("message-4");
		store.put(1, message1);
		store.put(2, message2);
		store.put(3, message3);
		assertEquals(3, store.size());
		assertEquals(message1, store.get(1));
		assertEquals(message2, store.get(2));
		assertEquals(message3, store.get(3));
		store.put(4, message4);
		assertEquals(3, store.size());
		assertNull(store.get(1));
		assertEquals(message2, store.get(2));
		assertEquals(message3, store.get(3));
		assertEquals(message4, store.get(4));
	}

}
