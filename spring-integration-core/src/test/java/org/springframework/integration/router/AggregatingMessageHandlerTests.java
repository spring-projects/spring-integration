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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Mark Fisher
 */
public class AggregatingMessageHandlerTests {

	private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();


	public AggregatingMessageHandlerTests() {
		this.executor.setMaxPoolSize(10);
		this.executor.setQueueCapacity(0);
		this.executor.afterPropertiesSet();
	}


	@Test
	public void testCompleteGroupWithinTimeout() throws InterruptedException {
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		executor.execute(new AggregatorTestTask(aggregator, message1, latch));
		executor.execute(new AggregatorTestTask(aggregator, message2, latch));
		executor.execute(new AggregatorTestTask(aggregator, message3, latch));
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("123456789", reply.getPayload());
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		aggregator.setTimeout(50);
		aggregator.setReaperInterval(10);
		aggregator.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage("123", "ABC", 2, 1, replyChannel);
		CountDownLatch latch = new CountDownLatch(1);
		AggregatorTestTask task = new AggregatorTestTask(aggregator, message, latch);
		executor.execute(task);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
		Message<?> discardedMessage = discardChannel.receive(500);
		assertNotNull(discardedMessage);
		assertEquals(message, discardedMessage);
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() throws InterruptedException {
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		aggregator.setTimeout(50);
		aggregator.setReaperInterval(10);
		aggregator.setSendPartialResultOnTimeout(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		CountDownLatch latch = new CountDownLatch(2);
		AggregatorTestTask task1 = new AggregatorTestTask(aggregator, message1, latch);
		AggregatorTestTask task2 = new AggregatorTestTask(aggregator, message2, latch);
		executor.execute(task1);
		executor.execute(task2);
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("handlers should have been invoked within time limit", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(500);
		assertNotNull("a reply message should have been received", reply);
		assertEquals("123456", reply.getPayload());
		assertNull(task1.getException());
		assertNull(task2.getException());
	}

	@Test
	public void testMultipleGroupsSimultaneously() throws InterruptedException {
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		QueueChannel replyChannel1 = new QueueChannel();
		QueueChannel replyChannel2 = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel1);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel1);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel1);
		Message<?> message4 = createMessage("abc", "XYZ", 3, 1, replyChannel2);
		Message<?> message5 = createMessage("def", "XYZ", 3, 2, replyChannel2);
		Message<?> message6 = createMessage("ghi", "XYZ", 3, 3, replyChannel2);
		CountDownLatch latch = new CountDownLatch(6);
		executor.execute(new AggregatorTestTask(aggregator, message1, latch));
		executor.execute(new AggregatorTestTask(aggregator, message6, latch));
		executor.execute(new AggregatorTestTask(aggregator, message2, latch));
		executor.execute(new AggregatorTestTask(aggregator, message5, latch));
		executor.execute(new AggregatorTestTask(aggregator, message3, latch));
		executor.execute(new AggregatorTestTask(aggregator, message4, latch));
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply1 = replyChannel1.receive(500);
		assertNotNull(reply1);
		assertEquals("123456789", reply1.getPayload());
		Message<?> reply2 = replyChannel2.receive(500);
		assertNotNull(reply2);
		assertEquals("abcdefghi", reply2.getPayload());
	}

	@Test
	public void testDiscardChannelForTrackedCorrelationId() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		aggregator.setDiscardChannel(discardChannel);
		aggregator.handle(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", discardChannel.receive(100).getPayload());
	}

	@Test
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		aggregator.setTrackedCorrelationIdCapacity(3);
		aggregator.setDiscardChannel(discardChannel);
		aggregator.handle(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-2", 2, 1, 1, replyChannel));
		assertEquals("test-2", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-3", 3, 1, 1, replyChannel));
		assertEquals("test-3", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", discardChannel.receive(100).getPayload());
	}

	@Test
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		aggregator.setTrackedCorrelationIdCapacity(3);
		aggregator.setDiscardChannel(discardChannel);
		aggregator.handle(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-2", 2, 1, 1, replyChannel));
		assertEquals("test-2", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-3", 3, 1, 1, replyChannel));
		assertEquals("test-3", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-4", 4, 1, 1, replyChannel));
		assertEquals("test-4", replyChannel.receive(100).getPayload());
		aggregator.handle(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", replyChannel.receive(100).getPayload());
		assertNull(discardChannel.receive(0));
	}

	@Test(expected=MessageHandlingException.class)
	public void testExceptionThrownIfNoCorrelationId() throws InterruptedException {
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(new TestAggregator());
		Message<?> message = createMessage("123", null, 2, 1, new QueueChannel());
		aggregator.handle(message);
	}


	private static Message<?> createMessage(String payload, Object correlationId,
			int sequenceSize, int sequenceNumber, MessageChannel replyChannel) {
		StringMessage message = new StringMessage(payload);
		message.getHeader().setCorrelationId(correlationId);
		message.getHeader().setSequenceSize(sequenceSize);
		message.getHeader().setSequenceNumber(sequenceNumber);
		message.getHeader().setReturnAddress(replyChannel);
		return message;
	}


	private static class TestAggregator implements Aggregator {

		public Message<?> aggregate(List<Message<?>> messages) {
			List<Message<?>> sortableList = new ArrayList<Message<?>>(messages);
			Collections.sort(sortableList, new MessageSequenceComparator());
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : sortableList) {
				buffer.append(message.getPayload().toString());
			}
			return new StringMessage(buffer.toString());
		}
	}


	private static class AggregatorTestTask implements Runnable {

		private AggregatingMessageHandler aggregator;

		private Message<?> message;

		private Exception exception;

		private CountDownLatch latch;


		AggregatorTestTask(AggregatingMessageHandler aggregator, Message<?> message, CountDownLatch latch) {
			this.aggregator = aggregator;
			this.message = message;
			this.latch = latch;
		}

		public Exception getException() {
			return this.exception;
		}

		public void run() {
			try {
				Message<?> result = this.aggregator.handle(message);
				if (result != null) {
					Object returnAddress = message.getHeader().getReturnAddress();
					if (returnAddress instanceof MessageChannel) {
						((MessageChannel) returnAddress).send(result);
					}
					else {
						throw new IllegalStateException("'returnAddress' was not a MessageChannel instance");
					}
				}
			}
			catch (Exception e) {
				this.exception = e;
			}
			finally {
				this.latch.countDown();
			}
		}
	}

}
