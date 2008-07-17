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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Marius Bogoevici
 */
public class ResequencerMessageHandlerTests {

	@Test
	public void testBasicResequencing() throws InterruptedException {
		ResequencingMessageHandler resequencer = new ResequencingMessageHandler();
		resequencer.setReleasePartialSequences(false);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 3, 3, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 3, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, 2, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		resequencer.handle(message1);
		resequencer.handle(message3);
		resequencer.handle(message2);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply1 = replyChannel.receive(500);
		Message<?> reply2 = replyChannel.receive(500);
		Message<?> reply3 = replyChannel.receive(500);
		assertNotNull(reply1);
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertNotNull(reply3);
		assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
	}

	@Test
	public void testResequencingWithIncompleteSequenceRelease() throws InterruptedException {
		ResequencingMessageHandler resequencer = new ResequencingMessageHandler();
		resequencer.setReleasePartialSequences(true);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, replyChannel);
		Message<?> message4 = createMessage("XYZ", "ABC", 4, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		resequencer.handle(message1);
		resequencer.handle(message2);
		resequencer.handle(message3);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply1 = replyChannel.receive(500);
		Message<?> reply2 = replyChannel.receive(500);
		Message<?> reply3 = replyChannel.receive(500);
		// only messages 1 and 2 must have been received by now
		assertNotNull(reply1);
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertNull(reply3);
		// when sending the last message, the whole sequence must have been sent
		latch = new CountDownLatch(1);
		resequencer.handle(message4);
		latch.await(1000, TimeUnit.MILLISECONDS);
		reply3 = replyChannel.receive(500);
		Message<?> reply4 = replyChannel.receive(500);
		assertNotNull(reply3);
		assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
		assertNotNull(reply4);
		assertEquals(new Integer(4), reply4.getHeaders().getSequenceNumber());
	}


	@Test
	public void testResequencingWithCompleteSequenceRelease() throws InterruptedException {
		ResequencingMessageHandler resequencer = new ResequencingMessageHandler();
		resequencer.setReleasePartialSequences(false);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 4, 2, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 4, 1, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 4, 4, replyChannel);
		Message<?> message4 = createMessage("XYZ", "ABC", 4, 3, replyChannel);
		CountDownLatch latch = new CountDownLatch(3);
		resequencer.handle(message1);
		resequencer.handle(message2);
		resequencer.handle(message3);
		latch.await(1000, TimeUnit.MILLISECONDS);
		Message<?> reply1 = replyChannel.receive(500);
		Message<?> reply2 = replyChannel.receive(500);
		Message<?> reply3 = replyChannel.receive(500);
		// no message must have been received by now
		assertNull(reply1);
		assertNull(reply2);
		assertNull(reply3);
		// when sending the last message, the whole sequence must have been sent
		latch = new CountDownLatch(1);
		resequencer.handle(message4);
		latch.await(1000, TimeUnit.MILLISECONDS);
		reply1 = replyChannel.receive(500);
		reply2 = replyChannel.receive(500);
		reply3 = replyChannel.receive(500);
		Message<?> reply4 = replyChannel.receive(500);
		assertNotNull(reply1);
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertNotNull(reply3);
		assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
		assertNotNull(reply4);
		assertEquals(new Integer(4), reply4.getHeaders().getSequenceNumber());
	}


	private static Message<?> createMessage(String payload, Object correlationId,
	                                        int sequenceSize, int sequenceNumber, MessageChannel replyChannel) {
		Message<String> message = MessageBuilder.fromPayload(payload)
				.setCorrelationId(correlationId)
				.setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber)
				.setReturnAddress(replyChannel)
				.build();
		return message;
	}

}
