/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Marius Bogoevici
 * @author Alex Peters
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class ResequencerTests {

	private ResequencingMessageHandler resequencer;

	private final ResequencingMessageGroupProcessor processor = new ResequencingMessageGroupProcessor();

	private final MessageGroupStore store = new SimpleMessageStore();

	@Before
	public void configureResequencer() {
		this.resequencer = new ResequencingMessageHandler(processor, store, null, null);
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();
	}

	@Test
	public void testBasicResequencing() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 3, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 2, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message2);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		assertNotNull(reply1);
		assertThat( new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber(), is(1));
		assertNotNull(reply2);
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber(), is(2));
		assertNotNull(reply3);
		assertThat( new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber(), is(3));
	}

	@Test
	public void testBasicResequencingA() throws InterruptedException {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);
		this.resequencer = new ResequencingMessageHandler(processor, store, null, releaseStrategy);
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);

		this.resequencer.handleMessage(message3);
		assertNull(replyChannel.receive(0));
		this.resequencer.handleMessage(message1);
		assertNotNull(replyChannel.receive(0));
		assertNull(replyChannel.receive(0));
	}

	@Test
	public void testBasicUnboundedResequencing() throws InterruptedException {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);
		this.resequencer = new ResequencingMessageHandler(processor, store, null, releaseStrategy);
		QueueChannel replyChannel = new QueueChannel();
		this.resequencer.setCorrelationStrategy(new CorrelationStrategy() {
			@Override
			public Object getCorrelationKey(Message<?> message) {
				return "A";
			}
		});
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();

		//Message<?> message0 = MessageBuilder.withPayload("0").setSequenceNumber(0).build();
		Message<?> message1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setReplyChannel(replyChannel).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setReplyChannel(replyChannel).build();
		Message<?> message3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setReplyChannel(replyChannel).build();
		Message<?> message4 = MessageBuilder.withPayload("4").setSequenceNumber(4).setReplyChannel(replyChannel).build();
		Message<?> message5 = MessageBuilder.withPayload("5").setSequenceNumber(5).setReplyChannel(replyChannel).build();

		this.resequencer.handleMessage(message3);
		assertNull(replyChannel.receive(0));
		this.resequencer.handleMessage(message1);
		assertNotNull(replyChannel.receive(0));

		this.resequencer.handleMessage(message2);

		assertNotNull(replyChannel.receive(0));
		assertNotNull(replyChannel.receive(0));
		assertNull(replyChannel.receive(0));

		this.resequencer.handleMessage(message5);
		assertNull(replyChannel.receive(0));
		this.resequencer.handleMessage(message4);
		assertNotNull(replyChannel.receive(0));
	}

	@Test
	public void testResequencingWithDuplicateMessages() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 3, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 2, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message2);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		assertNotNull(reply1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber());
		assertNotNull(reply3);
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber());
	}

	@Test
	public void testResequencingWithIncompleteSequenceRelease() throws InterruptedException {
		this.resequencer.setReleaseStrategy(new SequenceSizeReleaseStrategy(true));
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, replyChannel);
		Message<?> message4 = createMessage("XYZ", "ABC", 4, 3, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		this.resequencer.handleMessage(message3);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		// only messages 1 and 2 should have been received by now
		assertNotNull(reply1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber());
		assertNull(reply3);
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message4);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertNotNull(reply3);
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber());
		assertNotNull(reply4);
		assertEquals(new Integer(4), new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber());
	}

	@Test
	public void testResequencingWithPartialSequenceAndComparator() throws InterruptedException {
		this.resequencer.setReleaseStrategy(new SequenceSizeReleaseStrategy(true));
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("456", "ABC", 4, 2, replyChannel);
		Message<?> message2 = createMessage("123", "ABC", 4, 1, replyChannel);
		Message<?> message3 = createMessage("XYZ", "ABC", 4, 4, replyChannel);
		Message<?> message4 = createMessage("789", "ABC", 4, 3, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		this.resequencer.handleMessage(message3);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		// only messages 1 and 2 should have been received by now
		assertNotNull(reply1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber());
		assertNull(reply3);
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message4);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertNotNull(reply3);
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber());
		assertNotNull(reply4);
		assertEquals(new Integer(4), new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber());
	}

	@Test
	public void testResequencingWithDiscard() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, null);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, null);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		assertEquals(1, store.expireMessageGroups(-10000));
		Message<?> reply1 = discardChannel.receive(0);
		Message<?> reply2 = discardChannel.receive(0);
		Message<?> reply3 = discardChannel.receive(0);
		// only messages 1 and 2 should have been received by now
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertNull(reply3);
		ArrayList<Integer> sequence = new ArrayList<Integer>(Arrays.asList(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber(),
				new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()));
		Collections.sort(sequence);
		assertEquals("[1, 2]", sequence.toString());
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message3);
		reply3 = discardChannel.receive(0);
		assertNull(reply3);
	}

	@Test
	public void testResequencingWithDifferentSequenceSizes() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, null);
		Message<?> message2 = createMessage("456", "ABC", 5, 1, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		// this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
		Message<?> discard1 = discardChannel.receive(0);
		Message<?> discard2 = discardChannel.receive(0);
		// message2 has been discarded because it came in with the wrong sequence size
		assertNotNull(discard1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(discard1).getSequenceNumber());
		assertNull(discard2);
	}

	@Test
	public void testResequencingWithWrongSequenceSizeAndNumber() throws InterruptedException {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 2, 4, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.handleMessage(message1);
		// this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
		Message<?> reply1 = discardChannel.receive(0);
		// No message has been received - the message has been rejected.
		assertNull(reply1);
	}

	@Test
	public void testResequencingWithCompleteSequenceRelease() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, replyChannel);
		Message<?> message4 = createMessage("XYZ", "ABC", 4, 3, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		this.resequencer.handleMessage(message3);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		// no messages should have been received yet
		assertNull(reply1);
		assertNull(reply2);
		assertNull(reply3);
		// after sending the last message, the whole sequence should have been sent
		this.resequencer.handleMessage(message4);
		reply1 = replyChannel.receive(0);
		reply2 = replyChannel.receive(0);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertNotNull(reply1);
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber());
		assertNotNull(reply3);
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber());
		assertNotNull(reply4);
		assertEquals(new Integer(4), new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber());
	}

	@Test
	public void testRemovalOfBarrierWhenLastMessageOfSequenceArrives() {
		QueueChannel replyChannel = new QueueChannel();
		String correlationId = "ABC";
		Message<?> message1 = createMessage("123", correlationId, 1, 1, replyChannel);
		resequencer.handleMessage(message1);
		assertEquals(0, store.getMessageGroup(correlationId).size());
	}

	private static Message<?> createMessage(String payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(replyChannel).build();
	}

}
