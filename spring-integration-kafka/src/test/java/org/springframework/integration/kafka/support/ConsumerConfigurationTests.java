/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.support;

import junit.framework.Assert;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Soby Chacko
 */
public class ConsumerConfigurationTests {
    @Test
    @SuppressWarnings("unchecked")
    public void testReceiveMessageForSingleTopicFromSingleStream() {
        final ConsumerMetadata consumerMetadata = Mockito.mock(ConsumerMetadata.class);
        final ConsumerConnectionProvider consumerConnectionProvider =
                Mockito.mock(ConsumerConnectionProvider.class);
        final MessageLeftOverTracker messageLeftOverTracker = Mockito.mock(MessageLeftOverTracker.class);
        final ConsumerConnector consumerConnector = Mockito.mock(ConsumerConnector.class);

        Mockito.when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

        final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
                consumerConnectionProvider, messageLeftOverTracker);
        consumerConfiguration.setMaxMessages(1);

        final KafkaStream stream = Mockito.mock(KafkaStream.class);
        final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
        streams.add(stream);
        final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
        messageStreams.put("topic", streams);

        Mockito.when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
        final ConsumerIterator iterator = Mockito.mock(ConsumerIterator.class);
        Mockito.when(stream.iterator()).thenReturn(iterator);
        final MessageAndMetadata messageAndMetadata = Mockito.mock(MessageAndMetadata.class);
        Mockito.when(iterator.next()).thenReturn(messageAndMetadata);
        Mockito.when(messageAndMetadata.message()).thenReturn("got message");
        Mockito.when(messageAndMetadata.topic()).thenReturn("topic");
        Mockito.when(messageAndMetadata.partition()).thenReturn(1);

        final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
        Assert.assertEquals(messages.size(), 1);
        Assert.assertEquals(messages.get("topic").size(), 1);
        Assert.assertEquals(messages.get("topic").get(1).get(0), "got message");

        Mockito.verify(stream, Mockito.times(1)).iterator();
        Mockito.verify(iterator, Mockito.times(1)).next();
        Mockito.verify(messageAndMetadata, Mockito.times(1)).message();
        Mockito.verify(messageAndMetadata, Mockito.times(1)).topic();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiveMessageForSingleTopicFromMultipleStreams() {
        final ConsumerMetadata consumerMetadata = Mockito.mock(ConsumerMetadata.class);
        final ConsumerConnectionProvider consumerConnectionProvider =
                Mockito.mock(ConsumerConnectionProvider.class);
        final MessageLeftOverTracker messageLeftOverTracker = Mockito.mock(MessageLeftOverTracker.class);

        final ConsumerConnector consumerConnector = Mockito.mock(ConsumerConnector.class);

        Mockito.when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

        final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
                consumerConnectionProvider, messageLeftOverTracker);
        consumerConfiguration.setMaxMessages(3);

        final KafkaStream stream1 = Mockito.mock(KafkaStream.class);
        final KafkaStream stream2 = Mockito.mock(KafkaStream.class);
        final KafkaStream stream3 = Mockito.mock(KafkaStream.class);
        final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
        streams.add(stream1);
        streams.add(stream2);
        streams.add(stream3);
        final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
        messageStreams.put("topic", streams);

        Mockito.when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
        final ConsumerIterator iterator1 = Mockito.mock(ConsumerIterator.class);
        final ConsumerIterator iterator2 = Mockito.mock(ConsumerIterator.class);
        final ConsumerIterator iterator3 = Mockito.mock(ConsumerIterator.class);

        Mockito.when(stream1.iterator()).thenReturn(iterator1);
        Mockito.when(stream2.iterator()).thenReturn(iterator2);
        Mockito.when(stream3.iterator()).thenReturn(iterator3);
        final MessageAndMetadata messageAndMetadata1 = Mockito.mock(MessageAndMetadata.class);
        final MessageAndMetadata messageAndMetadata2 = Mockito.mock(MessageAndMetadata.class);
        final MessageAndMetadata messageAndMetadata3 = Mockito.mock(MessageAndMetadata.class);

        Mockito.when(iterator1.next()).thenReturn(messageAndMetadata1);
        Mockito.when(iterator2.next()).thenReturn(messageAndMetadata2);
        Mockito.when(iterator3.next()).thenReturn(messageAndMetadata3);

        Mockito.when(messageAndMetadata1.message()).thenReturn("got message");
        Mockito.when(messageAndMetadata1.topic()).thenReturn("topic");
        Mockito.when(messageAndMetadata1.partition()).thenReturn(1);

        Mockito.when(messageAndMetadata2.message()).thenReturn("got message");
        Mockito.when(messageAndMetadata2.topic()).thenReturn("topic");
        Mockito.when(messageAndMetadata2.partition()).thenReturn(2);

        Mockito.when(messageAndMetadata3.message()).thenReturn("got message");
        Mockito.when(messageAndMetadata3.topic()).thenReturn("topic");
        Mockito.when(messageAndMetadata3.partition()).thenReturn(3);

        final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
        Assert.assertEquals(messages.size(), 1);
        int sum = 0;

        final Map<Integer, List<Object>> values = messages.get("topic");

        for (final List<Object> l : values.values()) {
            sum += l.size();
        }

        Assert.assertEquals(sum, 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiveMessageForMultipleTopicsFromMultipleStreams() {
        final ConsumerMetadata consumerMetadata = Mockito.mock(ConsumerMetadata.class);
        final ConsumerConnectionProvider consumerConnectionProvider =
                Mockito.mock(ConsumerConnectionProvider.class);
        final MessageLeftOverTracker messageLeftOverTracker = Mockito.mock(MessageLeftOverTracker.class);

        final ConsumerConnector consumerConnector = Mockito.mock(ConsumerConnector.class);

        Mockito.when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

        final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
                consumerConnectionProvider, messageLeftOverTracker);
        consumerConfiguration.setMaxMessages(9);

        final KafkaStream stream1 = Mockito.mock(KafkaStream.class);
        final KafkaStream stream2 = Mockito.mock(KafkaStream.class);
        final KafkaStream stream3 = Mockito.mock(KafkaStream.class);
        final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
        streams.add(stream1);
        streams.add(stream2);
        streams.add(stream3);
        final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
        messageStreams.put("topic1", streams);
        messageStreams.put("topic2", streams);
        messageStreams.put("topic3", streams);

        Mockito.when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
        final ConsumerIterator iterator1 = Mockito.mock(ConsumerIterator.class);
        final ConsumerIterator iterator2 = Mockito.mock(ConsumerIterator.class);
        final ConsumerIterator iterator3 = Mockito.mock(ConsumerIterator.class);

        Mockito.when(stream1.iterator()).thenReturn(iterator1);
        Mockito.when(stream2.iterator()).thenReturn(iterator2);
        Mockito.when(stream3.iterator()).thenReturn(iterator3);
        final MessageAndMetadata messageAndMetadata1 = Mockito.mock(MessageAndMetadata.class);
        final MessageAndMetadata messageAndMetadata2 = Mockito.mock(MessageAndMetadata.class);
        final MessageAndMetadata messageAndMetadata3 = Mockito.mock(MessageAndMetadata.class);

        Mockito.when(iterator1.next()).thenReturn(messageAndMetadata1);
        Mockito.when(iterator2.next()).thenReturn(messageAndMetadata2);
        Mockito.when(iterator3.next()).thenReturn(messageAndMetadata3);

        Mockito.when(messageAndMetadata1.message()).thenReturn("got message1");
        Mockito.when(messageAndMetadata1.topic()).thenReturn("topic1");
        Mockito.when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

        Mockito.when(messageAndMetadata2.message()).thenReturn("got message2");
        Mockito.when(messageAndMetadata2.topic()).thenReturn("topic2");
        Mockito.when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

        Mockito.when(messageAndMetadata3.message()).thenReturn("got message3");
        Mockito.when(messageAndMetadata3.topic()).thenReturn("topic3");
        Mockito.when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

        final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
        int sum = 0;

        final Collection<Map<Integer, List<Object>>> values = messages.values();

        for (final Map<Integer, List<Object>> m : values) {
            for (final List<Object> l : m.values()) {
                sum += l.size();
            }
        }

        Assert.assertEquals(sum, 9);
    }

    private Answer<Object> getAnswer() {
        return new Answer<Object>() {
            private int count = 0;

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                if (count++ == 1) {
                    return 1;
                } else if (count++ == 2) {
                    return 2;
                }

                return 3;
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceiveMessageAndVerifyMessageLeftoverFromPreviousPollAreTakenFirst() {
        final ConsumerMetadata consumerMetadata = Mockito.mock(ConsumerMetadata.class);
        final ConsumerConnectionProvider consumerConnectionProvider =
                Mockito.mock(ConsumerConnectionProvider.class);
        final MessageLeftOverTracker messageLeftOverTracker = Mockito.mock(MessageLeftOverTracker.class);
        final ConsumerConnector consumerConnector = Mockito.mock(ConsumerConnector.class);

        Mockito.when(messageLeftOverTracker.getCurrentCount()).thenReturn(3);
        final MessageAndMetadata m1 = new MessageAndMetadata("key1", "value1", "topic1", 1, 1L);
        final MessageAndMetadata m2 = new MessageAndMetadata("key2", "value2", "topic2", 1, 1L);
        final MessageAndMetadata m3 = new MessageAndMetadata("key1", "value3", "topic3", 1, 1L);

        final List<MessageAndMetadata> mList = new ArrayList<MessageAndMetadata>();
        mList.add(m1);
        mList.add(m2);
        mList.add(m3);

        Mockito.when(messageLeftOverTracker.getMessageLeftOverFromPreviousPoll()).thenReturn(mList);

        Mockito.when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

        final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
                consumerConnectionProvider, messageLeftOverTracker);
        consumerConfiguration.setMaxMessages(5);

        final KafkaStream stream = Mockito.mock(KafkaStream.class);
        final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
        streams.add(stream);
        final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
        messageStreams.put("topic1", streams);

        Mockito.when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
        final ConsumerIterator iterator = Mockito.mock(ConsumerIterator.class);
        Mockito.when(stream.iterator()).thenReturn(iterator);
        final MessageAndMetadata messageAndMetadata = Mockito.mock(MessageAndMetadata.class);
        Mockito.when(iterator.next()).thenReturn(messageAndMetadata);
        Mockito.when(messageAndMetadata.message()).thenReturn("got message");
        Mockito.when(messageAndMetadata.topic()).thenReturn("topic1");
        Mockito.when(messageAndMetadata.partition()).thenReturn(1);

        final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
        int sum = 0;

        final Collection<Map<Integer, List<Object>>> values = messages.values();

        for (final Map<Integer, List<Object>> m : values) {
            for (final List<Object> l : m.values()) {
                sum += l.size();
            }

        }
        Assert.assertEquals(sum, 5);

        Assert.assertTrue(messages.containsKey("topic1"));
        Assert.assertTrue(messages.containsKey("topic2"));
        Assert.assertTrue(messages.containsKey("topic3"));

        Assert.assertTrue(valueFound(messages.get("topic1").get(1), "value1"));
        Assert.assertTrue(valueFound(messages.get("topic2").get(1), "value2"));
        Assert.assertTrue(valueFound(messages.get("topic3").get(1), "value3"));
    }

    private boolean valueFound(final List<Object> l, final String value){
        for (final Object o : l){
            if (value.equals(o)){
                return true;
            }
        }

        return false;
    }
}
