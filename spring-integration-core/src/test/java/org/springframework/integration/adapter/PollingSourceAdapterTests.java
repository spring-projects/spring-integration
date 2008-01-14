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

package org.springframework.integration.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class PollingSourceAdapterTests {

	@Test
	public void testPolledSourceSendsToChannel() {
		TestSource source = new TestSource("testing", 1);
		SimpleChannel channel = new SimpleChannel();
		PollingSourceAdapter<String> adapter = new PollingSourceAdapter<String>(source);
		adapter.setChannel(channel);
		adapter.setPeriod(100);
		adapter.start();
		Message<?> message = channel.receive();
		assertNotNull("message should not be null", message);
		assertEquals("testing.1", message.getPayload());
	}

	@Test
	public void testSendTimeout() {
		TestSource source = new TestSource("testing", 1);
		SimpleChannel channel = new SimpleChannel(1);
		PollingSourceAdapter<String> adapter = new PollingSourceAdapter<String>(source);
		adapter.setChannel(channel);
		adapter.setInitialDelay(10000);
		adapter.setSendTimeout(10);
		adapter.start();
		adapter.processMessages();
		adapter.processMessages();
		adapter.stop();
		Message<?> message1 = channel.receive();
		assertNotNull("message should not be null", message1);
		assertEquals("testing.1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull("second message should be null", message2);
		adapter.start();
		adapter.processMessages();
		Message<?> message3 = channel.receive(100);
		assertNotNull("third message should not be null", message3);
		assertEquals("testing.3", message3.getPayload());
	}

	@Test
	public void testMultipleMessagesPerPoll() {
		TestSource source = new TestSource("testing", 3);
		SimpleChannel channel = new SimpleChannel();
		PollingSourceAdapter<String> adapter = new PollingSourceAdapter<String>(source);
		adapter.setChannel(channel);
		adapter.setInitialDelay(10000);
		adapter.setMaxMessagesPerTask(5);
		adapter.start();
		adapter.processMessages();
		Message<?> message1 = channel.receive(0);
		assertNotNull("message should not be null", message1);
		assertEquals("testing.1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNotNull("message should not be null", message2);
		assertEquals("testing.2", message2.getPayload());
		Message<?> message3 = channel.receive(0);
		assertNotNull("message should not be null", message3);
		assertEquals("testing.3", message3.getPayload());		
		Message<?> message4 = channel.receive(0);
		assertNull("message should be null", message4);
	}

	@Test(expected=MessageHandlingException.class)
	public void testResultSizeExceedsLimit() {
		TestSource source = new TestSource("testing", 3);
		SimpleChannel channel = new SimpleChannel();
		PollingSourceAdapter<String> adapter = new PollingSourceAdapter<String>(source);
		adapter.setChannel(channel);
		adapter.setPeriod(1000);
		adapter.setMaxMessagesPerTask(2);
		adapter.start();
		adapter.processMessages();
	}


	private static class TestSource implements PollableSource<String> {

		private String message;

		private int messagesPerPoll;

		private AtomicInteger count = new AtomicInteger();

		public TestSource(String message, int messagesPerPoll) {
			this.message = message;
			this.messagesPerPoll = messagesPerPoll;
		}

		public Collection<String> poll(int limit) {
			List<String> results = new ArrayList<String>(this.messagesPerPoll);
			for (int i = 0; i < this.messagesPerPoll; i++) {
				results.add(message + "." + count.incrementAndGet());
			}
			return results;
		}
	}

}
