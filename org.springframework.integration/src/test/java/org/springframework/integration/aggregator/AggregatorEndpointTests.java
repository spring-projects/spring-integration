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

package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.TaskScheduler;

/**
 * @author Mark Fisher
 */
public class AggregatorEndpointTests {

	private TaskExecutor taskExecutor;

	private TaskScheduler taskScheduler;

	private AbstractMessageAggregator aggregator;


	@Before
	public void configureAggregator() {
		this.taskExecutor = new SimpleAsyncTaskExecutor();
		this.taskScheduler = new SimpleTaskScheduler(taskExecutor);
		this.aggregator = new TestAggregator();
		this.aggregator.setTaskScheduler(this.taskScheduler);
		this.taskScheduler.start();
		this.aggregator.start();
	}

	@Test
	public void testCompleteGroupWithinTimeout() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("123456789", reply.getPayload());
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setTimeout(50);
		this.aggregator.setReaperInterval(10);
		this.aggregator.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage("123", "ABC", 2, 1, replyChannel);
		CountDownLatch latch = new CountDownLatch(1);
		AggregatorTestTask task = new AggregatorTestTask(this.aggregator, message, latch);
		this.taskExecutor.execute(task);
		latch.await(2000, TimeUnit.MILLISECONDS);
		assertEquals("task should have completed within timeout", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(0);
		assertNull(reply);
		Message<?> discardedMessage = discardChannel.receive(2000);
		assertNotNull(discardedMessage);
		assertEquals(message, discardedMessage);
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() throws InterruptedException {
		this.aggregator.setTimeout(500);
		this.aggregator.setReaperInterval(10);
		this.aggregator.setSendPartialResultOnTimeout(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		CountDownLatch latch = new CountDownLatch(2);
		AggregatorTestTask task1 = new AggregatorTestTask(this.aggregator, message1, latch);
		AggregatorTestTask task2 = new AggregatorTestTask(this.aggregator, message2, latch);
		this.taskExecutor.execute(task1);
		this.taskExecutor.execute(task2);
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals("handlers should have been invoked within time limit", 0, latch.getCount());
		Message<?> reply = replyChannel.receive(3000);
		assertNotNull("a reply message should have been received", reply);
		assertEquals("123456", reply.getPayload());
		assertNull(task1.getException());
		assertNull(task2.getException());
	}

	@Test
	public void testMultipleGroupsSimultaneously() throws InterruptedException {
		QueueChannel replyChannel1 = new QueueChannel();
		QueueChannel replyChannel2 = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel1);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel1);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel1);
		Message<?> message4 = createMessage("abc", "XYZ", 3, 1, replyChannel2);
		Message<?> message5 = createMessage("def", "XYZ", 3, 2, replyChannel2);
		Message<?> message6 = createMessage("ghi", "XYZ", 3, 3, replyChannel2);
		CountDownLatch latch = new CountDownLatch(6);
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message6, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message5, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message4, latch));
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
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.onMessage(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", discardChannel.receive(100).getPayload());
	}

	@Test
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.onMessage(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-2", 2, 1, 1, replyChannel));
		assertEquals("test-2", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-3", 3, 1, 1, replyChannel));
		assertEquals("test-3", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", discardChannel.receive(100).getPayload());
	}

	@Test
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.onMessage(createMessage("test-1a", 1, 1, 1, replyChannel));
		assertEquals("test-1a", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-2", 2, 1, 1, replyChannel));
		assertEquals("test-2", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-3", 3, 1, 1, replyChannel));
		assertEquals("test-3", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-4", 4, 1, 1, replyChannel));
		assertEquals("test-4", replyChannel.receive(100).getPayload());
		this.aggregator.onMessage(createMessage("test-1b", 1, 1, 1, replyChannel));
		assertEquals("test-1b", replyChannel.receive(100).getPayload());
		assertNull(discardChannel.receive(0));
	}

	@Test(expected = MessageHandlingException.class)
	public void testExceptionThrownIfNoCorrelationId() throws InterruptedException {
		Message<?> message = createMessage("123", null, 2, 1, new QueueChannel());
		this.aggregator.onMessage(message);
	}
	
	@Test
	public void testAdditionalMessageAfterCompletion() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);
		Message<?> message4 = createMessage("abc", "ABC", 3, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(4);
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message4, latch));
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(500);
		assertNotNull(reply);
		assertEquals("123456789".length(), ((String)reply.getPayload()).length());
	}
	
	@Test
	public void testNullReturningAggregator() throws InterruptedException {
		this.aggregator = new NullReturningAggregator();
		this.aggregator.setTaskScheduler(this.taskScheduler);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		AggregatorTestTask task1 = new AggregatorTestTask(aggregator, message1, latch);
		this.taskExecutor.execute(task1);	
		AggregatorTestTask task2 = new AggregatorTestTask(aggregator, message2, latch);
		this.taskExecutor.execute(task2);
		AggregatorTestTask task3 = new AggregatorTestTask(aggregator, message3, latch);
		this.taskExecutor.execute(task3);
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertNull(task1.getException());
		assertNull(task2.getException());
		assertNull(task3.getException());
		Message<?> reply = replyChannel.receive(500);
		assertNull(reply);
		assertTrue(((NullReturningAggregator) this.aggregator).isAggregationComplete());
	}


	private static Message<?> createMessage(String payload, Object correlationId,
			int sequenceSize, int sequenceNumber, MessageChannel replyChannel) {
		Message<String> message = MessageBuilder.withPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReturnAddress(replyChannel)
				.build();
		return message;
	}


	private static class TestAggregator extends AbstractMessageAggregator {
		
		public Message<?> aggregateMessages(List<Message<?>> messages) {
			List<Message<?>> sortableList = new ArrayList<Message<?>>(messages);
			Collections.sort(sortableList, new MessageSequenceComparator());
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : sortableList) {
				buffer.append(message.getPayload().toString());
			}
			return new StringMessage(buffer.toString());
		}
		
	}


	private static class NullReturningAggregator extends AbstractMessageAggregator {

		private boolean aggregationComplete;
	
		
		public boolean isAggregationComplete() {
			return aggregationComplete;
		}
		

		public Message<?> aggregateMessages(List<Message<?>> messages) {
			this.aggregationComplete = true;
			return null;
		}
	
	}


	private static class AggregatorTestTask implements Runnable {

		private AbstractMessageAggregator aggregator;

		private Message<?> message;

		private Exception exception;

		private CountDownLatch latch;


		AggregatorTestTask(AbstractMessageAggregator aggregator, Message<?> message, CountDownLatch latch) {
			this.aggregator = aggregator;
			this.message = message;
			this.latch = latch;
		}

		public Exception getException() {
			return this.exception;
		}

		public void run() {
			try {
				this.aggregator.onMessage(message);
			}
			catch (Exception e) {
				this.exception = e;
			}
			finally {
				this.latch.countDown();
			}
		}
	}
	
	@After
	public void stopTaskScheduler() {
		this.taskScheduler.stop();
		this.aggregator.stop();
	}

}
