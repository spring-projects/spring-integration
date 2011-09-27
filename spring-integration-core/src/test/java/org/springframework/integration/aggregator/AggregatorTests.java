/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class AggregatorTests {

	private AggregatingMessageHandler aggregator;

	private SimpleMessageStore store = new SimpleMessageStore(50);


	@Before
	public void configureAggregator() {
		this.aggregator = new AggregatingMessageHandler(new MultiplyingProcessor(), store);
	}


	@Test
	public void testCompleteGroupWithinTimeout() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(3);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(2000);
		assertNotNull(reply);
		assertEquals(reply.getPayload(), 105);
	}

	@Test
	public void testShouldNotSendPartialResultOnTimeoutByDefault() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		this.aggregator.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = createMessage(3, "ABC", 2, 1, replyChannel, null);
		this.aggregator.handleMessage(message);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(100);
		assertNull("No message should have been sent normally", reply);
		Message<?> discardedMessage = discardChannel.receive(1000);
		assertNotNull("A message should have been discarded", discardedMessage);
		assertEquals(message, discardedMessage);
	}

	@Test
	public void testShouldSendPartialResultOnTimeoutTrue() throws InterruptedException {
		this.aggregator.setSendPartialResultOnExpiry(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.store.expireMessageGroups(-10000);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull("A reply message should have been received", reply);
		assertEquals(15, reply.getPayload());
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
		aggregator.handleMessage(message1);
		aggregator.handleMessage(message5);
		aggregator.handleMessage(message3);
		aggregator.handleMessage(message6);
		aggregator.handleMessage(message4);
		aggregator.handleMessage(message2);
		@SuppressWarnings("unchecked")
		Message<Integer> reply1 = (Message<Integer>) replyChannel1.receive(500);
		assertNotNull(reply1);
		assertThat(reply1.getPayload(), is(105));
		@SuppressWarnings("unchecked")
		Message<Integer> reply2 = (Message<Integer>) replyChannel2.receive(500);
		assertNotNull(reply2);
		assertThat(reply2.getPayload(), is(2431));
	}

	@Test
	@Ignore
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityAtLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		// this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertEquals(1, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(3, 2, 1, 1, replyChannel, null));
		assertEquals(3, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(4, 3, 1, 1, replyChannel, null));
		assertEquals(4, replyChannel.receive(100).getPayload());
		// next message with same correllation ID is discarded
		this.aggregator.handleMessage(createMessage(2, 1, 1, 1, replyChannel, null));
		assertEquals(2, discardChannel.receive(100).getPayload());
	}

	@Test
	@Ignore
	// dropped backwards compatibility for setting capacity limit (it's always Integer.MAX_VALUE)
	public void testTrackedCorrelationIdsCapacityPassesLimit() {
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		// this.aggregator.setTrackedCorrelationIdCapacity(3);
		this.aggregator.setDiscardChannel(discardChannel);
		this.aggregator.handleMessage(createMessage(1, 1, 1, 1, replyChannel, null));
		assertEquals(1, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(2, 2, 1, 1, replyChannel, null));
		assertEquals(2, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(3, 3, 1, 1, replyChannel, null));
		assertEquals(3, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(4, 4, 1, 1, replyChannel, null));
		assertEquals(4, replyChannel.receive(100).getPayload());
		this.aggregator.handleMessage(createMessage(5, 1, 1, 1, replyChannel, null));
		assertEquals(5, replyChannel.receive(100).getPayload());
		assertNull(discardChannel.receive(0));
	}

	@Test(expected = MessageHandlingException.class)
	public void testExceptionThrownIfNoCorrelationId() throws InterruptedException {
		Message<?> message = createMessage(3, null, 2, 1, new QueueChannel(), null);
		this.aggregator.handleMessage(message);
	}

	@Test
	public void testAdditionalMessageAfterCompletion() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(4);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);
		this.aggregator.handleMessage(message4);
		latch.await(1000, TimeUnit.MILLISECONDS);
		// small wait to make sure the fourth message is received
		Thread.sleep(10);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull("A message should be aggregated", reply);
		assertThat(((Integer) reply.getPayload()), is(105));
	}

	@Test
	public void shouldRejectDuplicatedSequenceNumbers() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		Message<?> message4 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		CountDownLatch latch = new CountDownLatch(4);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message3);
		// duplicated sequence number, either message3 or message4 should be rejected
		this.aggregator.handleMessage(message4);
		this.aggregator.handleMessage(message2);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull("A message should be aggregated", reply);
		assertThat(((Integer) reply.getPayload()), is(105));
	}

	@Test
	public void testNullReturningAggregator() throws InterruptedException {
		this.aggregator = new AggregatingMessageHandler(new NullReturningMessageProcessor(), new SimpleMessageStore(50));
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage(3, "ABC", 3, 1, replyChannel, null);
		Message<?> message2 = createMessage(5, "ABC", 3, 2, replyChannel, null);
		Message<?> message3 = createMessage(7, "ABC", 3, 3, replyChannel, null);
		this.aggregator.handleMessage(message1);
		this.aggregator.handleMessage(message2);
		this.aggregator.handleMessage(message3);
		Message<?> reply = replyChannel.receive(500);
		assertNull(reply);
	}


	private static Message<?> createMessage(Object payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel, String predefinedId) {
		MessageBuilder<Object> builder = MessageBuilder.withPayload(payload).setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize).setSequenceNumber(sequenceNumber).setReplyChannel(replyChannel);
		if (predefinedId != null) {
			builder.setHeader(MessageHeaders.ID, predefinedId);
		}
		return builder.build();
	}


	private class MultiplyingProcessor implements MessageGroupProcessor {
		public Object processMessageGroup(MessageGroup group) {
			Integer product = 1;
			for (Message<?> message : group.getUnmarked()) {
				product *= (Integer) message.getPayload();
			}
			return product;
		}
	}


	private class NullReturningMessageProcessor implements MessageGroupProcessor {
		public Object processMessageGroup(MessageGroup group) {
			return null;
		}
	}

}
