/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

	// although this test has no assertions it results in ConcurrentModificationException
	// if executed before changes for INT-2508
	@Test
	public void testPriorityChannelWithConcurrentModification() throws Exception{
		final PriorityChannel channel = new PriorityChannel();
		final Message<String> message = new GenericMessage<String>("hello");
		for (int i = 0; i < 1000; i++) {
			channel.send(message);
			new Thread(new Runnable() {
				public void run() {
					channel.receive();
				}
			}).start();
			new Thread(new Runnable() {
				public void run() {
					message.getHeaders().toString();
				}
			}).start();
		}
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
	public void testWithCustomComparatorAndSequence() {
		PriorityChannel channel = new PriorityChannel(10, new FooHeaderComparator());
		Message<?> message1 = MessageBuilder.withPayload(1).setHeader("foo", 1).build();
		Message<?> message2 = MessageBuilder.withPayload(2).setHeader("foo", 1).build();
		Message<?> message3 = MessageBuilder.withPayload(3).setHeader("foo", 1).build();
		Message<?> message4 = MessageBuilder.withPayload(4).build();
		Message<?> message5 = MessageBuilder.withPayload(5).setHeader("foo", 3).build();

		Message<?> message6 = MessageBuilder.withPayload(6).setHeader("foo", 3).build();
		Message<?> message7 = MessageBuilder.withPayload(7).setHeader("foo", 4).build();
		Message<?> message8 = MessageBuilder.withPayload(8).setHeader("foo", 4).build();


		channel.send(message1);
		channel.send(message2);
		channel.send(message3);
		channel.send(message4);
		channel.send(message5);
		channel.send(message6);
		channel.send(message7);
		channel.send(message8);

		Object receivedOne = channel.receive(0).getPayload();
		Object receivedTwo = channel.receive(0).getPayload();
		Object receivedThree = channel.receive(0).getPayload();
		Object receivedFour = channel.receive(0).getPayload();
		Object receivedFive = channel.receive(0).getPayload();
		Object receivedSix = channel.receive(0).getPayload();
		Object receivedSeven = channel.receive(0).getPayload();
		Object receivedEight = channel.receive(0).getPayload();

		assertEquals(7, receivedOne);
		assertEquals(8, receivedTwo);
		assertEquals(5, receivedThree);
		assertEquals(6, receivedFour);
		assertEquals(1, receivedFive);
		assertEquals(2, receivedSix);
		assertEquals(3, receivedSeven);
		assertEquals(4, receivedEight);
	}

	@Test
	public void testWithDefaultComparatorAndSequence() {
		PriorityChannel channel = new PriorityChannel();
		Message<?> message1 = MessageBuilder.withPayload(1).setPriority(1).build();
		Message<?> message2 = MessageBuilder.withPayload(2).setPriority(1).build();
		Message<?> message3 = MessageBuilder.withPayload(3).setPriority(1).build();
		Message<?> message4 = MessageBuilder.withPayload(4).setPriority(2).build();
		Message<?> message5 = MessageBuilder.withPayload(5).setPriority(2).build();

		Message<?> message6 = MessageBuilder.withPayload(6).build();
		Message<?> message7 = MessageBuilder.withPayload(7).build();

		channel.send(message1);
		channel.send(message2);
		channel.send(message3);
		channel.send(message4);
		channel.send(message5);
		channel.send(message6);
		channel.send(message7);

		Object receivedOne = channel.receive(0).getPayload();
		Object receivedTwo = channel.receive(0).getPayload();
		Object receivedThree = channel.receive(0).getPayload();
		Object receivedFour = channel.receive(0).getPayload();
		Object receivedFive = channel.receive(0).getPayload();
		Object receivedSix = channel.receive(0).getPayload();
		Object receivedSeven = channel.receive(0).getPayload();

		assertEquals(4, receivedOne);
		assertEquals(5, receivedTwo);
		assertEquals(1, receivedThree);
		assertEquals(2, receivedFour);
		assertEquals(3, receivedFive);
		assertEquals(6, receivedSix);
		assertEquals(7, receivedSeven);		
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
		assertEquals("test:NULL", channel.receive(0).getPayload());
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
		assertEquals("test:NULL", channel.receive(0).getPayload());
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
	
	public static class FooHeaderComparator implements Comparator<Message<?>> {
		public int compare(Message<?> message1, Message<?> message2) {
			Integer foo1 = (Integer) message1.getHeaders().get("foo");
			Integer foo2 = (Integer) message2.getHeaders().get("foo");
			foo1 = foo1 != null ? foo1 : 0;
			foo2 = foo2 != null ? foo2 : 0;
			return foo2.compareTo(foo1);
		}	
	}

}
