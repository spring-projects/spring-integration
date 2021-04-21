/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Artem Bilan
 */
public class ConcurrentAggregatorTests {

	private TaskExecutor taskExecutor;

	private AggregatingMessageHandler aggregator;

	private final MessageGroupStore store = new SimpleMessageStore();


	@BeforeEach
	public void configureAggregator() {
		this.taskExecutor = new SimpleAsyncTaskExecutor();
		this.aggregator = new AggregatingMessageHandler(new MultiplyingProcessor(), this.store);
		this.aggregator.setReleaseStrategy(new SimpleSequenceSizeReleaseStrategy());
	}


	@Test
	public void testCompleteGroupWithinTimeout() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(3);
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(latch.getCount()).isEqualTo(0L);
		Message<?> reply = replyChannel.receive(2000);
		assertThat(reply).isNotNull();
		assertThat(105).isEqualTo(reply.getPayload());
	}

	@Test
	@Disabled
	// dropped backwards compatibility for duplicate ID's
	public void testCompleteGroupWithinTimeoutWithSameId() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, "ID#1");
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, "ID#1");
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, "ID#1");
		CountDownLatch latch = new CountDownLatch(3);
		// for testing the duplication scenario, the messages must be processed
		// synchronously
		new AggregatorTestTask(this.aggregator, message1, latch).run();
		new AggregatorTestTask(this.aggregator, message2, latch).run();
		new AggregatorTestTask(this.aggregator, message3, latch).run();
		Message<?> reply = replyChannel.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("123456789");
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage(3, "ABC", 2, 1, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(1);
		AggregatorTestTask task = new AggregatorTestTask(this.aggregator, message, latch);
		this.taskExecutor.execute(task);

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(latch.getCount()).as("Task should have completed within timeout").isEqualTo(0);
		Message<?> reply = replyChannel.receive(10);
		assertThat(reply).as("No message should have been sent normally").isNull();
		this.store.expireMessageGroups(-10000);
		Message<?> discardedMessage = discardChannel.receive(10000);
		assertThat(discardedMessage).as("A message should have been discarded").isNotNull();
		assertThat(discardedMessage).isEqualTo(message);
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() throws InterruptedException {
		this.aggregator.setSendPartialResultOnExpiry(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(2);
		AggregatorTestTask task1 = new AggregatorTestTask(this.aggregator, message1, latch);
		AggregatorTestTask task2 = new AggregatorTestTask(this.aggregator, message2, latch);
		this.taskExecutor.execute(task1);
		this.taskExecutor.execute(task2);

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(latch.getCount()).as("handlers should have been invoked within time limit").isEqualTo(0);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(1000);
		assertThat(reply).as("A reply message should have been received").isNotNull();
		assertThat(reply.getPayload()).isEqualTo(15);
		assertThat(task1.getException()).isNull();
		assertThat(task2.getException()).isNull();
	}

	@Test
	public void testMultipleGroupsSimultaneously() throws InterruptedException {
		QueueChannel replyChannel1 = new QueueChannel();
		QueueChannel replyChannel2 = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel1, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel1, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel1, null);
		Message<?> message4 = createMessage(11, "XYZ", 3, 1, replyChannel2, null);
		Message<?> message5 = createMessage(13, "XYZ", 3, 2, replyChannel2, null);
		Message<?> message6 = createMessage(17, "XYZ", 3, 3, replyChannel2, null);
		CountDownLatch latch = new CountDownLatch(6);
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message6, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message5, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message4, latch));

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		@SuppressWarnings("unchecked")
		Message<Integer> reply1 = (Message<Integer>) replyChannel1.receive(1000);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo(105);
		@SuppressWarnings("unchecked")
		Message<Integer> reply2 = (Message<Integer>) replyChannel2.receive(1000);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo(2431);
	}

	@Test
	@Disabled
	// dropped backwards compatibility for setting capacity limit (it's always
	// Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		// this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(1);
		this.aggregator.handleMessage(createMessage(3, 2, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(3);
		this.aggregator.handleMessage(createMessage(4, 3, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(4);
		// next message with same correlation ID is discarded
		this.aggregator.handleMessage(createMessage(2, 1, 1, 1, replyChannel, null));
		assertThat(discardChannel.receive(1000).getPayload()).isEqualTo(2);
	}

	@Test
	@Disabled
	// dropped backwards compatibility for setting capacity limit (it's always
	// Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		// this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(1);
		this.aggregator.handleMessage(createMessage(2, 2, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(2);
		this.aggregator.handleMessage(createMessage(3, 3, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(3);
		this.aggregator.handleMessage(createMessage(4, 4, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(4);
		this.aggregator.handleMessage(createMessage(5, 1, 1, 1, replyChannel, null));
		assertThat(replyChannel.receive(1000).getPayload()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void testExceptionThrownIfNoCorrelationId() {
		Message<?> message = createMessage(3, null, 2, 1, new QueueChannel(), null);
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.aggregator.handleMessage(message));
	}

	@Test
	public void testAdditionalMessageAfterCompletion() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(4);

		this.aggregator.setReleaseStrategy(new SequenceSizeReleaseStrategy());

		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message1, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message2, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message3, latch));
		this.taskExecutor.execute(new AggregatorTestTask(this.aggregator, message4, latch));

		assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();

		Message<?> reply = replyChannel.receive(10000);
		assertThat(reply).as("A message should be aggregated").isNotNull();
		assertThat(reply.getPayload()).isEqualTo(105);
	}


	private static Message<?> createMessage(Object payload,
			Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel, String predefinedId) {

		MessageBuilder<Object> builder =
				MessageBuilder.withPayload(payload)
						.setCorrelationId(correlationId).setSequenceSize(sequenceSize)
						.setSequenceNumber(sequenceNumber)
						.setReplyChannel(replyChannel);
		if (predefinedId != null) {
			builder.setHeader(MessageHeaders.ID, predefinedId);
		}
		return builder.build();
	}


	private static class AggregatorTestTask implements Runnable {

		private final MessageHandler aggregator;

		private final Message<?> message;

		private Exception exception;

		private final CountDownLatch latch;

		AggregatorTestTask(MessageHandler aggregator, Message<?> message,
				CountDownLatch latch) {
			this.aggregator = aggregator;
			this.message = message;
			this.latch = latch;
		}

		public Exception getException() {
			return this.exception;
		}

		@Override
		public void run() {
			try {
				this.aggregator.handleMessage(message);
			}
			catch (Exception e) {
				this.exception = e;
			}
			finally {
				this.latch.countDown();
			}
		}

	}


	private class MultiplyingProcessor implements MessageGroupProcessor {

		MultiplyingProcessor() {
			super();
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			Integer product = 1;
			for (Message<?> message : group.getMessages()) {
				product *= (Integer) message.getPayload();
			}
			return product;
		}

	}


	@SuppressWarnings("unused")
	private class NullReturningMessageProcessor implements MessageGroupProcessor {

		NullReturningMessageProcessor() {
			super();
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			return null;
		}

	}

}
