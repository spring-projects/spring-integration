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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Marius Bogoevici
 * @author Alex Peters
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ResequencerTests {

	private ResequencingMessageHandler resequencer;

	private final ResequencingMessageGroupProcessor processor = new ResequencingMessageGroupProcessor();

	private final MessageGroupStore store = new SimpleMessageStore();

	@BeforeEach
	public void configureResequencer() {
		this.resequencer = new ResequencingMessageHandler(processor, store, null, null);
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();
	}

	@Test
	public void testBasicResequencing() {
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
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(reply3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber()).isEqualTo(3);
	}

	@Test
	public void testBasicResequencingA() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);
		this.resequencer = new ResequencingMessageHandler(processor, store, null, releaseStrategy);
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();

		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);

		this.resequencer.handleMessage(message3);
		assertThat(replyChannel.receive(0)).isNull();
		this.resequencer.handleMessage(message1);
		assertThat(replyChannel.receive(0)).isNotNull();
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void testBasicUnboundedResequencing() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		releaseStrategy.setReleasePartialSequences(true);
		this.resequencer = new ResequencingMessageHandler(processor, store, null, releaseStrategy);
		QueueChannel replyChannel = new QueueChannel();
		this.resequencer.setCorrelationStrategy(message -> "A");
		this.resequencer.setBeanFactory(mock(BeanFactory.class));
		this.resequencer.afterPropertiesSet();

		//Message<?> message0 = MessageBuilder.withPayload("0").setSequenceNumber(0).build();
		Message<?> message1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setReplyChannel(replyChannel).build();
		Message<?> message2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setReplyChannel(replyChannel).build();
		Message<?> message3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setReplyChannel(replyChannel).build();
		Message<?> message4 = MessageBuilder.withPayload("4").setSequenceNumber(4).setReplyChannel(replyChannel).build();
		Message<?> message5 = MessageBuilder.withPayload("5").setSequenceNumber(5).setReplyChannel(replyChannel).build();

		this.resequencer.handleMessage(message3);
		assertThat(replyChannel.receive(0)).isNull();
		this.resequencer.handleMessage(message1);
		assertThat(replyChannel.receive(0)).isNotNull();

		this.resequencer.handleMessage(message2);

		assertThat(replyChannel.receive(0)).isNotNull();
		assertThat(replyChannel.receive(0)).isNotNull();
		assertThat(replyChannel.receive(0)).isNull();

		this.resequencer.handleMessage(message5);
		assertThat(replyChannel.receive(0)).isNull();
		this.resequencer.handleMessage(message4);
		assertThat(replyChannel.receive(0)).isNotNull();
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
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(reply3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber()).isEqualTo(3);
	}

	@Test
	public void testResequencingWithIncompleteSequenceRelease() {
		this.resequencer.setReleaseStrategy(new SequenceSizeReleaseStrategy(true));
		// INT-3846
		this.resequencer.setMessageStore(new SimpleMessageStore(3));
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 4, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 1, replyChannel); // release 2 after this one
		Message<?> message4 = createMessage("XYZ", "ABC", 4, 3, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		this.resequencer.handleMessage(message3);
		Message<?> reply1 = replyChannel.receive(0);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		// only messages 1 and 2 should have been received by now
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(reply3).isNull();
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message4);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertThat(reply3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber()).isEqualTo(3);
		assertThat(reply4).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber()).isEqualTo(4);
	}

	@Test
	public void testResequencingWithCapacity() {
		this.resequencer.setReleaseStrategy(new SequenceSizeReleaseStrategy(true));
		// INT-3846
		this.resequencer.setMessageStore(new SimpleMessageStore(3, 2));
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 4, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 1, replyChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		try {
			this.resequencer.handleMessage(message3);
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage()).contains("out of capacity (2) for group 'ABC'");
		}
	}

	@Test
	public void testResequencingWithPartialSequenceAndComparator() {
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
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(reply3).isNull();
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message4);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertThat(reply3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber()).isEqualTo(3);
		assertThat(reply4).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber()).isEqualTo(4);
	}

	@Test
	public void testResequencingWithDiscard() {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, null);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, null);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		assertThat(store.expireMessageGroups(-10000)).isEqualTo(1);
		Message<?> reply1 = discardChannel.receive(0);
		Message<?> reply2 = discardChannel.receive(0);
		Message<?> reply3 = discardChannel.receive(0);
		// only messages 1 and 2 should have been received by now
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply3).isNull();
		ArrayList<Integer> sequence = new ArrayList<>(
				Arrays.asList(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber(),
						new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()));
		Collections.sort(sequence);
		assertThat(sequence.toString()).isEqualTo("[1, 2]");
		// Once a group is expired, late messages are discarded immediately by default
		this.resequencer.handleMessage(message3);
		reply3 = discardChannel.receive(0);
		assertThat(reply3).isNotNull();
	}

	@Test
	public void testResequencingWithDifferentSequenceSizes() {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, null);
		Message<?> message2 = createMessage("456", "ABC", 5, 1, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.setReleasePartialSequences(true); // force SequenceSizeReleaseStrategy
		this.resequencer.handleMessage(message1);
		this.resequencer.handleMessage(message2);
		// this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
		Message<?> discard1 = discardChannel.receive(0);
		Message<?> discard2 = discardChannel.receive(0);
		// message2 has been discarded because it came in with the wrong sequence size
		assertThat(discard1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(discard1).getSequenceNumber()).isEqualTo(1);
		assertThat(discard2).isNull();
	}

	@Test
	public void testResequencingWithWrongSequenceSizeAndNumber() {
		QueueChannel discardChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 2, 4, null);
		this.resequencer.setSendPartialResultOnExpiry(false);
		this.resequencer.setDiscardChannel(discardChannel);
		this.resequencer.handleMessage(message1);
		// this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
		Message<?> reply1 = discardChannel.receive(0);
		// No message has been received - the message has been rejected.
		assertThat(reply1).isNull();
	}

	@Test
	public void testResequencingWithCompleteSequenceRelease() {
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
		assertThat(reply1).isNull();
		assertThat(reply2).isNull();
		assertThat(reply3).isNull();
		// after sending the last message, the whole sequence should have been sent
		this.resequencer.handleMessage(message4);
		reply1 = replyChannel.receive(0);
		reply2 = replyChannel.receive(0);
		reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply1).getSequenceNumber()).isEqualTo(1);
		assertThat(reply2).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply2).getSequenceNumber()).isEqualTo(2);
		assertThat(reply3).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply3).getSequenceNumber()).isEqualTo(3);
		assertThat(reply4).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(reply4).getSequenceNumber()).isEqualTo(4);
	}

	@Test
	public void testRemovalOfBarrierWhenLastMessageOfSequenceArrives() {
		QueueChannel replyChannel = new QueueChannel();
		String correlationId = "ABC";
		Message<?> message1 = createMessage("123", correlationId, 1, 1, replyChannel);
		resequencer.handleMessage(message1);
		assertThat(store.getMessageGroup(correlationId).size()).isEqualTo(0);
	}

	@Test
	public void testTimeoutDefaultExpiry() {
		this.resequencer.setGroupTimeoutExpression(new SpelExpressionParser().parseExpression("100"));
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		this.resequencer.setTaskScheduler(taskScheduler);
		QueueChannel discardChannel = new QueueChannel();
		this.resequencer.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		this.resequencer.setOutputChannel(replyChannel);

		Message<?> message3 = createMessage("789", "ABC", 3, 3, null);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, null);
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message2);
		Message<?> out1 = replyChannel.receive(10);
		assertThat(out1).isNull();
		out1 = discardChannel.receive(20000);
		assertThat(out1).isNotNull();
		Message<?> out2 = discardChannel.receive(10);
		assertThat(out2).isNotNull();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, null);
		this.resequencer.handleMessage(message1);
		Message<?> out3 = discardChannel.receive(0);
		assertThat(out3).isNotNull();
	}

	@Test
	public void testTimeoutDontExpire() {
		this.resequencer.setGroupTimeoutExpression(new SpelExpressionParser().parseExpression("100"));
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		this.resequencer.setTaskScheduler(taskScheduler);
		QueueChannel discardChannel = new QueueChannel();
		this.resequencer.setDiscardChannel(discardChannel);
		QueueChannel replyChannel = new QueueChannel();
		this.resequencer.setOutputChannel(replyChannel);
		this.resequencer.setExpireGroupsUponTimeout(true);

		Message<?> message3 = createMessage("789", "ABC", 3, 3, null);
		Message<?> message2 = createMessage("456", "ABC", 3, 2, null);
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message2);
		Message<?> out1 = replyChannel.receive(0);
		assertThat(out1).isNull();
		out1 = discardChannel.receive(20_000);
		assertThat(out1).isNotNull();
		Message<?> out2 = discardChannel.receive(10_000);
		assertThat(out2).isNotNull();
		Message<?> message1 = createMessage("123", "ABC", 3, 1, null);
		this.resequencer.handleMessage(message1);
		Message<?> out3 = discardChannel.receive(0);
		assertThat(out3).isNull();
		out3 = discardChannel.receive(10_000);
		assertThat(out3).isNotNull();
	}

	private static Message<?> createMessage(String payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel) {

		return MessageBuilder.withPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReplyChannel(replyChannel)
				.build();
	}

}
