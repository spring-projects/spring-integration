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

package org.springframework.integration.adapter.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class CharacterStreamSourceAdapterTests {

	@Test
	public void testEndOfStream() {
		byte[] bytes = "test".getBytes();
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		MessageChannel channel = new SimpleChannel();
		CharacterStreamSourceAdapter adapter = new CharacterStreamSourceAdapter(stream);
		adapter.setChannel(channel);
		adapter.start();
		int count = adapter.processMessages();
		assertEquals(1, count);
		Message<?> message1 = channel.receive(0);
		assertEquals("test", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		adapter.processMessages();
		Message<?> message3 = channel.receive(0);
		assertNull(message3);
	}

	@Test
	public void testEndOfStreamWithMaxMessagesPerTask() {
		byte[] bytes = "test".getBytes();
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		MessageChannel channel = new SimpleChannel();
		CharacterStreamSourceAdapter adapter = new CharacterStreamSourceAdapter(stream);
		adapter.setChannel(channel);
		adapter.setMaxMessagesPerTask(5);
		adapter.start();
		int count = adapter.processMessages();
		assertEquals(1, count);
		Message<?> message1 = channel.receive(0);
		assertEquals("test", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
	}

	@Test
	public void testMultipleLinesWithSingleMessagePerTask() {
		String s = "test1" + System.getProperty("line.separator") + "test2";
		ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes());
		MessageChannel channel = new SimpleChannel();
		CharacterStreamSourceAdapter adapter = new CharacterStreamSourceAdapter(stream);
		adapter.setInitialDelay(10000);
		adapter.setMaxMessagesPerTask(1);
		adapter.setChannel(channel);
		adapter.start();
		int count = adapter.processMessages();
		assertEquals(1, count);
		Message<?> message1 = channel.receive(0);
		assertEquals("test1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		adapter.processMessages();
		Message<?> message3 = channel.receive(0);
		assertEquals("test2", message3.getPayload());
	}

	@Test
	public void testLessThanMaxMessagesAvailable() {
		String s = "test1" + System.getProperty("line.separator") + "test2";
		ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes());
		MessageChannel channel = new SimpleChannel();
		CharacterStreamSourceAdapter adapter = new CharacterStreamSourceAdapter(stream);
		adapter.setChannel(channel);
		adapter.setMaxMessagesPerTask(5);
		adapter.start();
		int count = adapter.processMessages();
		assertEquals(2, count);
		Message<?> message1 = channel.receive(0);
		assertEquals("test1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertEquals("test2", message2.getPayload());
		Message<?> message3 = channel.receive(0);
		assertNull(message3);
	}

}
