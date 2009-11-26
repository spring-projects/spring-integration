/*
 * Copyright 2002-2009 the original author or authors.
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

import org.junit.After;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.matchers.JUnitMatchers.*;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static java.util.Arrays.*;

/**
 * @author Marius Bogoevici
 * @author Alex Peters
 * @author Iwein Fuld
 */
public class NewResequencerTests {

    private CorrelatingMessageHandler resequencer;

    private ThreadPoolTaskScheduler taskScheduler;

    private DefaultResequencerStrategies resequencerStrategies;

    @Before
    public void configureResequencer() {
        this.resequencerStrategies = new DefaultResequencerStrategies();
        MessageStore store = new SimpleMessageStore(30);
        this.resequencer = new CorrelatingMessageHandler(store, resequencerStrategies, resequencerStrategies, resequencerStrategies);
        this.taskScheduler = TestUtils.createTaskScheduler(10);
        this.resequencer.setTaskScheduler(taskScheduler);
        this.taskScheduler.afterPropertiesSet();
        this.resequencer.start();
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
        assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
        assertNotNull(reply2);
        assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
        assertNotNull(reply3);
        assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
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
        assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
        assertNotNull(reply2);
        assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
        assertNotNull(reply3);
        assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
    }


    @Test
    public void testResequencingWithIncompleteSequenceRelease() throws InterruptedException {
        this.resequencerStrategies.setReleasePartialSequences(true);
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
        assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
        assertNotNull(reply2);
        assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
        assertNull(reply3);
        // when sending the last message, the whole sequence must have been sent
        this.resequencer.handleMessage(message4);
        reply3 = replyChannel.receive(0);
        Message<?> reply4 = replyChannel.receive(0);
        assertNotNull(reply3);
        assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
        assertNotNull(reply4);
        assertEquals(new Integer(4), reply4.getHeaders().getSequenceNumber());
    }

    @Test
    public void testResequencingWithDiscard() throws InterruptedException {
        QueueChannel replyChannel = new QueueChannel();
        QueueChannel discardChannel = new QueueChannel();
        Message<?> message1 = createMessage("123", "ABC", 3, 2, replyChannel);
        Message<?> message2 = createMessage("456", "ABC", 3, 1, replyChannel);
        Message<?> message3 = createMessage("789", "ABC", 3, 3, replyChannel);
        this.resequencer.setSendPartialResultOnTimeout(false);
        this.resequencerStrategies.setReleasePartialSequences(false);
        this.resequencer.setDiscardChannel(discardChannel);
        this.resequencer.setTimeout(90000);
        this.resequencer.handleMessage(message1);
        this.resequencer.handleMessage(message2);
        this.resequencer.forceComplete("ABC");
        Message<?> reply1 = discardChannel.receive(0);
        Message<?> reply2 = discardChannel.receive(0);
        Message<?> reply3 = discardChannel.receive(0);
        // messages 1 and 2 should have been received by now in no particular order
        assertNotNull(reply1);
        assertNotNull(reply2);
        Integer sequenceNo1 = reply1.getHeaders().getSequenceNumber();
        Integer sequenceNo2 = reply2.getHeaders().getSequenceNumber();
        assertThat(asList(sequenceNo1, sequenceNo2), hasItems(1, 2));
        assertNull(reply3);
        // when sending the last message, it waits in the buffer for retries of the other two
        this.resequencer.handleMessage(message3);
        reply3 = discardChannel.receive(0);
        assertNull(reply3);
        this.resequencer.handleMessage(message1);
        this.resequencer.handleMessage(message2);
        reply1 = replyChannel.receive(0);
        reply2 = replyChannel.receive(0);
        reply3 = replyChannel.receive(0);
        assertNotNull(reply1);
        assertThat(reply1.getHeaders().getSequenceNumber(), is(new Integer(1)));
        assertNotNull(reply2);
        assertThat(reply2.getHeaders().getSequenceNumber(), is(new Integer(2)));
        assertNotNull(reply3);
        assertThat(reply3.getHeaders().getSequenceNumber(), is(new Integer(3)));
    }


    @Test
    @Ignore
    //different sequence sizes are not supported
    public void testResequencingWithDifferentSequenceSizes() throws InterruptedException {
        QueueChannel replyChannel = new QueueChannel();
        QueueChannel discardChannel = new QueueChannel();
        Message<?> message1 = createMessage("123", "ABC", 4, 2, replyChannel);
        Message<?> message2 = createMessage("456", "ABC", 5, 1, replyChannel);
        this.resequencer.setSendPartialResultOnTimeout(false);
        //this.resequencer.setReleasePartialSequences(false);
        this.resequencer.setDiscardChannel(discardChannel);
        this.resequencer.setTimeout(90000);
        this.resequencer.handleMessage(message1);
        this.resequencer.handleMessage(message2);
        //this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
        Message<?> reply1 = discardChannel.receive(0);
        Message<?> reply2 = discardChannel.receive(0);
        // only messages 1 - with sequence number 2 - should have been received by now
        // the other has been discarded
        assertNotNull(reply1);
        assertEquals(new Integer(2), reply1.getHeaders().getSequenceNumber());
        assertNull(reply2);
    }

    @Test
    public void testResequencingWithWrongSequenceSizeAndNumber() throws InterruptedException {
        QueueChannel replyChannel = new QueueChannel();
        QueueChannel discardChannel = new QueueChannel();
        Message<?> message1 = createMessage("123", "ABC", 2, 4, replyChannel);
        this.resequencer.setSendPartialResultOnTimeout(false);
        //this.resequencer.setReleasePartialSequences(false);
        this.resequencer.setDiscardChannel(discardChannel);
        this.resequencer.setTimeout(90000);
        this.resequencer.handleMessage(message1);
        //this.resequencer.discardBarrier(this.resequencer.barriers.get("ABC"));
        Message<?> reply1 = discardChannel.receive(0);
        // No message has been received - the message has been rejected.
        assertNull(reply1);
    }

    @Test
    public void testResequencingWithCompleteSequenceRelease() throws InterruptedException {
        //this.resequencer.setReleasePartialSequences(false);
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
        assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
        assertNotNull(reply2);
        assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
        assertNotNull(reply3);
        assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
        assertNotNull(reply4);
        assertEquals(new Integer(4), reply4.getHeaders().getSequenceNumber());
    }

    @Test
    public void testRemovalOfBarrierWhenLastMessageOfSequenceArrives() {
        QueueChannel replyChannel = new QueueChannel();
        String correlationId = "ABC";
        Message<?> message1 = createMessage("123", correlationId, 1, 1,
                replyChannel);
        resequencer.handleMessage(message1);
        //assertThat(resequencer.barriers.containsKey(correlationId), is(false));
    }


    private static Message<?> createMessage(String payload, Object correlationId,
                                            int sequenceSize, int sequenceNumber, MessageChannel replyChannel) {
        return MessageBuilder.withPayload(payload)
                .setCorrelationId(correlationId)
                .setSequenceSize(sequenceSize)
                .setSequenceNumber(sequenceNumber)
                .setReplyChannel(replyChannel)
                .build();
    }

    @After
    public void stopTaskScheduler() {
        this.resequencer.stop();
        this.taskScheduler.destroy();
    }

}
