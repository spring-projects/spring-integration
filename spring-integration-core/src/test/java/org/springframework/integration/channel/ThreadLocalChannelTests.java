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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ThreadLocalChannelTests {

	@Test
	public void testSendAndReceive() {
		ThreadLocalChannel channel = new ThreadLocalChannel();
		StringMessage message = new StringMessage("test");
		assertNull(channel.receive());
		assertTrue(channel.send(message));
		Message<?> response = channel.receive();
		assertNotNull(response);
		assertEquals(response, message);
		assertNull(channel.receive());
	}

	@Test
	public void testSendAndClear() {
		ThreadLocalChannel channel = new ThreadLocalChannel();
		StringMessage message1 = new StringMessage("test1");
		StringMessage message2 = new StringMessage("test2");
		assertNull(channel.receive());
		assertTrue(channel.send(message1));
		assertTrue(channel.send(message2));
		List<Message<?>> clearedMessages = channel.clear();
		assertEquals(2, clearedMessages.size());
		assertEquals(message1, clearedMessages.get(0));
		assertEquals(message2, clearedMessages.get(1));
		assertNull(channel.receive());
	}

}
