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

package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class PriorityChannelTests {

	@Test
	public void testCapacityEnforced() {
		PriorityChannel channel = new PriorityChannel(3);
		assertTrue(channel.send(new GenericMessage<String>("test1"), 0));
		assertTrue(channel.send(new GenericMessage<String>("test2"), 0));
		assertTrue(channel.send(new GenericMessage<String>("test3"), 0));
		assertFalse(channel.send(new GenericMessage<String>("test4"), 0));
		channel.receive(0);
		assertTrue(channel.send(new GenericMessage<String>("test5")));
	}
	
	@Test
	public void testDefaultComparatorWithTimestampFallback() throws Exception{
		PriorityChannel channel = new PriorityChannel();
		for (int i = 0; i < 1000; i++) {
			channel.send(new GenericMessage<Integer>(i));
		}
		for (int i = 0; i < 1000; i++) {
			assertEquals(i, channel.receive().getPayload());
		}
	}

	@Test
	public void testDefaultComparator() {
		PriorityChannel channel = new PriorityChannel(5);
		Message<?> priority1 = createPriorityMessage(10);
		Message<?> priority2 = createPriorityMessage(7);
		Message<?> priority3 = createPriorityMessage(0);
		Message<?> priority4 = createPriorityMessage(-3);
		Message<?> priority5 = createPriorityMessage(-99);
		channel.send(priority4);
		channel.send(priority3);
		channel.send(priority5);
		channel.send(priority1);
		channel.send(priority2);
		assertEquals("test:10", channel.receive(0).getPayload());
		assertEquals("test:7", channel.receive(0).getPayload());
		assertEquals("test:0", channel.receive(0).getPayload());
		assertEquals("test:-3", channel.receive(0).getPayload());
		assertEquals("test:-99", channel.receive(0).getPayload());
	}

	@Test
	public void testCustomComparator() {
		PriorityChannel channel = new PriorityChannel(5, new StringPayloadComparator());
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		Message<?> messageC = new GenericMessage<String>("C");
		Message<?> messageD = new GenericMessage<String>("D");
		Message<?> messageE = new GenericMessage<String>("E");
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

	@Test
	public void testNullPriorityIsConsideredNormal() {
		PriorityChannel channel = new PriorityChannel(5);
		Message<?> highPriority = createPriorityMessage(5);
		Message<?> lowPriority = createPriorityMessage(-5);
		Message<?> nullPriority = new GenericMessage<String>("test:NULL");
		channel.send(lowPriority);
		channel.send(highPriority);
		channel.send(nullPriority);
		assertEquals("test:5", channel.receive(0).getPayload());
		//assertEquals("test:NULL", channel.receive(0).getPayload());
		assertEquals("test:-5", channel.receive(0).getPayload());
	}

	@Test
	public void testUnboundedCapacity() {
		PriorityChannel channel = new PriorityChannel();
		Message<?> highPriority = createPriorityMessage(5);
		Message<?> lowPriority = createPriorityMessage(-5);
		Message<?> nullPriority = new GenericMessage<String>("test:NULL");
		channel.send(lowPriority);
		channel.send(highPriority);
		channel.send(nullPriority);
		assertEquals("test:5", channel.receive(0).getPayload());
		//assertEquals("test:NULL", channel.receive(0).getPayload());
		assertEquals("test:-5", channel.receive(0).getPayload());
	}

	@Test
	public void testTimeoutElapses() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Executor executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<String>("test-1"));
		executor.execute(new Runnable() {
			public void run() {
				sentSecondMessage.set(channel.send(new GenericMessage<String>("test-2"), 10));
				latch.countDown();
			}
		});
		assertFalse(sentSecondMessage.get());
		Thread.sleep(500);
		Message<?> message1 = channel.receive();
		assertNotNull(message1);
		assertEquals("test-1", message1.getPayload());
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertFalse(sentSecondMessage.get());
		assertNull(channel.receive(0));
	}

	@Test
	public void testTimeoutDoesNotElapse() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Executor executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<String>("test-1"));
		executor.execute(new Runnable() {
			public void run() {
				sentSecondMessage.set(channel.send(new GenericMessage<String>("test-2"), 3000));
				latch.countDown();
			}
		});
		assertFalse(sentSecondMessage.get());
		Thread.sleep(500);
		Message<?> message1 = channel.receive();
		assertNotNull(message1);
		assertEquals("test-1", message1.getPayload());
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(sentSecondMessage.get());
		Message<?> message2 = channel.receive();
		assertNotNull(message2);
		assertEquals("test-2", message2.getPayload());
	}

	@Test
	public void testIndefiniteTimeout() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		Executor executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<String>("test-1"));
		executor.execute(new Runnable() {
			public void run() {
				sentSecondMessage.set(channel.send(new GenericMessage<String>("test-2"), -1));
				latch.countDown();
			}
		});
		assertFalse(sentSecondMessage.get());
		Thread.sleep(500);
		Message<?> message1 = channel.receive();
		assertNotNull(message1);
		assertEquals("test-1", message1.getPayload());
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(sentSecondMessage.get());
		Message<?> message2 = channel.receive();
		assertNotNull(message2);
		assertEquals("test-2", message2.getPayload());
	}


	private static Message<String> createPriorityMessage(int priority) {
		return MessageBuilder.withPayload("test:" + priority).setPriority(priority).build(); 
	}


	public static class StringPayloadComparator implements Comparator<Message<?>> {

		public int compare(Message<?> message1, Message<?> message2) {
			String s1 = (String) message1.getPayload();
			String s2 = (String) message2.getPayload();
			return s1.compareTo(s2);
		}	
	}

}
