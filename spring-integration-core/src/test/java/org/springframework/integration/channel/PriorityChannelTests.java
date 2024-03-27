/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PriorityChannelTests {

	@Test
	public void testCapacityEnforced() {
		PriorityChannel channel = new PriorityChannel(3);
		assertThat(channel.send(new GenericMessage<>("test1"), 0)).isTrue();
		assertThat(channel.send(new GenericMessage<>("test2"), 0)).isTrue();
		assertThat(channel.send(new GenericMessage<>("test3"), 0)).isTrue();
		assertThat(channel.send(new GenericMessage<>("test4"), 0)).isFalse();
		channel.receive(0);
		assertThat(channel.send(new GenericMessage<>("test5"))).isTrue();
	}

	@Test
	public void testDefaultComparatorWithTimestampFallback() {
		PriorityChannel channel = new PriorityChannel();
		for (int i = 0; i < 1000; i++) {
			channel.send(new GenericMessage<>(i));
		}
		for (int i = 0; i < 1000; i++) {
			assertThat(channel.receive().getPayload()).isEqualTo(i);
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
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:10");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:7");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:0");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:-3");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:-99");
	}

	@Test
	public void testPriorityChannelWithConcurrentModification() throws InterruptedException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		final PriorityChannel channel = new PriorityChannel();
		final Message<String> message = new GenericMessage<>("hello");
		for (int i = 0; i < 1000; i++) {
			channel.send(message);
			executorService.execute(channel::receive);
			executorService.execute(() -> message.getHeaders().toString());
		}

		executorService.shutdown();
		assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testCustomComparator() {
		PriorityChannel channel = new PriorityChannel(5, new StringPayloadComparator());
		Message<?> messageA = new GenericMessage<>("A");
		Message<?> messageB = new GenericMessage<>("B");
		Message<?> messageC = new GenericMessage<>("C");
		Message<?> messageD = new GenericMessage<>("D");
		Message<?> messageE = new GenericMessage<>("E");
		channel.send(messageC);
		channel.send(messageA);
		channel.send(messageE);
		channel.send(messageD);
		channel.send(messageB);
		assertThat(channel.receive(0).getPayload()).isEqualTo("A");
		assertThat(channel.receive(0).getPayload()).isEqualTo("B");
		assertThat(channel.receive(0).getPayload()).isEqualTo("C");
		assertThat(channel.receive(0).getPayload()).isEqualTo("D");
		assertThat(channel.receive(0).getPayload()).isEqualTo("E");
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

		assertThat(receivedOne).isEqualTo(7);
		assertThat(receivedTwo).isEqualTo(8);
		assertThat(receivedThree).isEqualTo(5);
		assertThat(receivedFour).isEqualTo(6);
		assertThat(receivedFive).isEqualTo(1);
		assertThat(receivedSix).isEqualTo(2);
		assertThat(receivedSeven).isEqualTo(3);
		assertThat(receivedEight).isEqualTo(4);
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

		assertThat(receivedOne).isEqualTo(4);
		assertThat(receivedTwo).isEqualTo(5);
		assertThat(receivedThree).isEqualTo(1);
		assertThat(receivedFour).isEqualTo(2);
		assertThat(receivedFive).isEqualTo(3);
		assertThat(receivedSix).isEqualTo(6);
		assertThat(receivedSeven).isEqualTo(7);
	}

	@Test
	public void testNullPriorityIsConsideredNormal() {
		PriorityChannel channel = new PriorityChannel(5);
		Message<?> highPriority = createPriorityMessage(5);
		Message<?> lowPriority = createPriorityMessage(-5);
		Message<?> nullPriority = new GenericMessage<>("test:NULL");
		channel.send(lowPriority);
		channel.send(highPriority);
		channel.send(nullPriority);
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:5");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:NULL");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:-5");
	}

	@Test
	public void testUnboundedCapacity() {
		PriorityChannel channel = new PriorityChannel();
		Message<?> highPriority = createPriorityMessage(5);
		Message<?> lowPriority = createPriorityMessage(-5);
		Message<?> nullPriority = new GenericMessage<>("test:NULL");
		channel.send(lowPriority);
		channel.send(highPriority);
		channel.send(nullPriority);
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:5");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:NULL");
		assertThat(channel.receive(0).getPayload()).isEqualTo("test:-5");
	}

	@Test
	public void testTimeoutElapses() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<>("test-1"));
		executor.execute(() -> sentSecondMessage.set(channel.send(new GenericMessage<>("test-2"), 10)));
		assertThat(sentSecondMessage.get()).isFalse();

		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		Message<?> message1 = channel.receive(10000);
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("test-1");
		assertThat(sentSecondMessage.get()).isFalse();
		assertThat(channel.receive(0)).isNull();
	}

	@Test
	public void testTimeoutDoesNotElapse() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		final CountDownLatch latch = new CountDownLatch(1);
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<>("test-1"));
		executor.execute(() -> {
			sentSecondMessage.set(channel.send(new GenericMessage<>("test-2"), 3000));
			latch.countDown();
		});
		assertThat(sentSecondMessage.get()).isFalse();
		Thread.sleep(10);
		Message<?> message1 = channel.receive();
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("test-1");
		assertThat(latch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(sentSecondMessage.get()).isTrue();
		Message<?> message2 = channel.receive();
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("test-2");
		executor.shutdownNow();
	}

	@Test
	public void testIndefiniteTimeout() throws InterruptedException {
		final PriorityChannel channel = new PriorityChannel(1);
		final AtomicBoolean sentSecondMessage = new AtomicBoolean(false);
		ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		channel.send(new GenericMessage<>("test-1"));
		executor.execute(() -> sentSecondMessage.set(channel.send(new GenericMessage<>("test-2"), -1)));
		assertThat(sentSecondMessage.get()).isFalse();
		Thread.sleep(10);
		Message<?> message1 = channel.receive(10000);
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("test-1");
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		assertThat(sentSecondMessage.get()).isTrue();
		Message<?> message2 = channel.receive();
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("test-2");
	}

	private static Message<String> createPriorityMessage(int priority) {
		return MessageBuilder.withPayload("test:" + priority).setPriority(priority).build();
	}

	public static class StringPayloadComparator implements Comparator<Message<?>> {

		@Override
		public int compare(Message<?> message1, Message<?> message2) {
			String s1 = (String) message1.getPayload();
			String s2 = (String) message2.getPayload();
			return s1.compareTo(s2);
		}

	}

	public static class FooHeaderComparator implements Comparator<Message<?>> {

		@Override
		public int compare(Message<?> message1, Message<?> message2) {
			Integer foo1 = (Integer) message1.getHeaders().get("foo");
			Integer foo2 = (Integer) message2.getHeaders().get("foo");
			foo1 = foo1 != null ? foo1 : 0;
			foo2 = foo2 != null ? foo2 : 0;
			return foo2.compareTo(foo1);
		}

	}

}
