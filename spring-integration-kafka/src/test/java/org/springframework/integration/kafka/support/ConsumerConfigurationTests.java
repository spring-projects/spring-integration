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

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.*;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.Decoder;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @since 0.5
 */
public class ConsumerConfigurationTests<K,V> {

	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForSingleTopicFromSingleStream() {
		final ConsumerMetadata<K,V> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<K,V> messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		Map<String, Integer> topicStreamMap = new HashMap<String, Integer>();
		topicStreamMap.put("topic1", 1);
		when(consumerMetadata.getTopicStreamMap()).thenReturn(topicStreamMap);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<K,V> consumerConfiguration = new ConsumerConfiguration<K,V>(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(1);

		final KafkaStream<K,V> stream = mock(KafkaStream.class);
		final List<KafkaStream<K,V>> streams = new ArrayList<KafkaStream<K,V>>();
		streams.add(stream);
		final Map<String, List<KafkaStream<K,V>>> messageStreams = new HashMap<String, List<KafkaStream<K,V>>>();
		messageStreams.put("topic", streams);

		when(consumerConfiguration.createMessageStreamsForTopic()).thenReturn(messageStreams);
		final ConsumerIterator<K,V> iterator = mock(ConsumerIterator.class);
		when(stream.iterator()).thenReturn(iterator);
		final MessageAndMetadata<K,V> messageAndMetadata = mock(MessageAndMetadata.class);
		when(iterator.next()).thenReturn(messageAndMetadata);
		when(messageAndMetadata.message()).thenReturn((V) "got message");
		when(messageAndMetadata.topic()).thenReturn("topic");
		when(messageAndMetadata.partition()).thenReturn(1);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		Assert.assertEquals(1, messages.size());
		Assert.assertEquals(1, messages.get("topic").size());
		Assert.assertEquals("got message", messages.get("topic").get(1).get(0));

		verify(stream, times(1)).iterator();
		verify(iterator, times(1)).next();
		verify(messageAndMetadata, times(1)).message();
		verify(messageAndMetadata, times(1)).topic();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForSingleTopicFromMultipleStreams() {
		final ConsumerMetadata<K,V> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<K,V> messageLeftOverTracker = mock(MessageLeftOverTracker.class);

		Map<String, Integer> topicStreamMap = new HashMap<String, Integer>();
		topicStreamMap.put("topic1", 1);
		when(consumerMetadata.getTopicStreamMap()).thenReturn(topicStreamMap);

		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<K,V> consumerConfiguration = new ConsumerConfiguration<K,V>(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(3);

		final KafkaStream<K,V> stream1 = mock(KafkaStream.class);
		final KafkaStream<K,V> stream2 = mock(KafkaStream.class);
		final KafkaStream<K,V> stream3 = mock(KafkaStream.class);
		final List<KafkaStream<K,V>> streams = new ArrayList<KafkaStream<K,V>>();
		streams.add(stream1);
		streams.add(stream2);
		streams.add(stream3);
		final Map<String, List<KafkaStream<K,V>>> messageStreams = new HashMap<String, List<KafkaStream<K,V>>>();
		messageStreams.put("topic", streams);

		when(consumerConfiguration.createMessageStreamsForTopic()).thenReturn(messageStreams);
		final ConsumerIterator<K,V> iterator1 = mock(ConsumerIterator.class);
		final ConsumerIterator<K,V> iterator2 = mock(ConsumerIterator.class);
		final ConsumerIterator<K,V> iterator3 = mock(ConsumerIterator.class);

		when(stream1.iterator()).thenReturn(iterator1);
		when(stream2.iterator()).thenReturn(iterator2);
		when(stream3.iterator()).thenReturn(iterator3);
		final MessageAndMetadata<K,V> messageAndMetadata1 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<K,V> messageAndMetadata2 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<K,V> messageAndMetadata3 = mock(MessageAndMetadata.class);

		when(iterator1.next()).thenReturn(messageAndMetadata1);
		when(iterator2.next()).thenReturn(messageAndMetadata2);
		when(iterator3.next()).thenReturn(messageAndMetadata3);

		when(messageAndMetadata1.message()).thenReturn((V)"got message");
		when(messageAndMetadata1.topic()).thenReturn("topic");
		when(messageAndMetadata1.partition()).thenReturn(1);

		when(messageAndMetadata2.message()).thenReturn((V)"got message");
		when(messageAndMetadata2.topic()).thenReturn("topic");
		when(messageAndMetadata2.partition()).thenReturn(2);

		when(messageAndMetadata3.message()).thenReturn((V)"got message");
		when(messageAndMetadata3.topic()).thenReturn("topic");
		when(messageAndMetadata3.partition()).thenReturn(3);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		Assert.assertEquals(messages.size(), 1);
		int sum = 0;

		final Map<Integer, List<Object>> values = messages.get("topic");

		for (final List<Object> l : values.values()) {
			sum += l.size();
		}

		Assert.assertEquals(3, sum);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForMultipleTopicsFromMultipleStreams() {
		final ConsumerMetadata<K,V> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
        final MessageLeftOverTracker<K, V> messageLeftOverTracker = mock(MessageLeftOverTracker.class);

		Map<String, Integer> topicStreamMap = new HashMap<String, Integer>();
		topicStreamMap.put("topic1", 1);
		when(consumerMetadata.getTopicStreamMap()).thenReturn(topicStreamMap);


		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<K,V> consumerConfiguration = new ConsumerConfiguration<K,V>(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(9);

		final KafkaStream<K,V> stream1 = mock(KafkaStream.class);
		final KafkaStream<K,V> stream2 = mock(KafkaStream.class);
		final KafkaStream<K,V> stream3 = mock(KafkaStream.class);
		final List<KafkaStream<K,V>> streams = new ArrayList<KafkaStream<K,V>>();
		streams.add(stream1);
		streams.add(stream2);
		streams.add(stream3);
		final Map<String, List<KafkaStream<K,V>>> messageStreams = new HashMap<String, List<KafkaStream<K,V>>>();
		messageStreams.put("topic1", streams);
		messageStreams.put("topic2", streams);
		messageStreams.put("topic3", streams);

		when(consumerConfiguration.createMessageStreamsForTopic()).thenReturn(messageStreams);
		final ConsumerIterator<K,V> iterator1 = mock(ConsumerIterator.class);
		final ConsumerIterator<K,V> iterator2 = mock(ConsumerIterator.class);
		final ConsumerIterator<K,V> iterator3 = mock(ConsumerIterator.class);

		when(stream1.iterator()).thenReturn(iterator1);
		when(stream2.iterator()).thenReturn(iterator2);
		when(stream3.iterator()).thenReturn(iterator3);
		final MessageAndMetadata<K,V> messageAndMetadata1 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<K,V> messageAndMetadata2 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<K,V> messageAndMetadata3 = mock(MessageAndMetadata.class);

		when(iterator1.next()).thenReturn(messageAndMetadata1);
		when(iterator2.next()).thenReturn(messageAndMetadata2);
		when(iterator3.next()).thenReturn(messageAndMetadata3);

		when(messageAndMetadata1.message()).thenReturn((V)"got message1");
		when(messageAndMetadata1.topic()).thenReturn("topic1");
		when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

		when(messageAndMetadata2.message()).thenReturn((V)"got message2");
		when(messageAndMetadata2.topic()).thenReturn("topic2");
		when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

		when(messageAndMetadata3.message()).thenReturn((V)"got message3");
		when(messageAndMetadata3.topic()).thenReturn("topic3");
		when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();

		int sum = 0;

		final Collection<Map<Integer, List<Object>>> values = messages.values();

		for (final Map<Integer, List<Object>> m : values) {
			for (final List<Object> l : m.values()) {
				sum += l.size();
			}
		}

		Assert.assertEquals(9, sum);
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
		final ConsumerMetadata<K,V> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<K,V> messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		Map<String, Integer> topicStreamMap = new HashMap<String, Integer>();
		topicStreamMap.put("topic1", 1);
		when(consumerMetadata.getTopicStreamMap()).thenReturn(topicStreamMap);
		when(messageLeftOverTracker.getCurrentCount()).thenReturn(3);

        final MessageAndMetadata<String, String> m1 = mock(MessageAndMetadata.class);
        final MessageAndMetadata<String, String> m2 = mock(MessageAndMetadata.class);
        final MessageAndMetadata<String, String> m3 = mock(MessageAndMetadata.class);

        when(m1.key()).thenReturn("key1");
        when(m1.message()).thenReturn("value1");
        when(m1.topic()).thenReturn("topic1");
        when(m1.partition()).thenReturn(1);

        when(m2.key()).thenReturn("key2");
        when(m2.message()).thenReturn("value2");
        when(m2.topic()).thenReturn("topic2");
        when(m2.partition()).thenReturn(1);

        when(m3.key()).thenReturn("key1");
        when(m3.message()).thenReturn("value3");
        when(m3.topic()).thenReturn("topic3");
        when(m3.partition()).thenReturn(1);

		final List<MessageAndMetadata<String, String>> mList = new ArrayList<MessageAndMetadata<String, String>>();
		mList.add(m1);
		mList.add(m2);
		mList.add(m3);

		when((List<MessageAndMetadata<String, String>>) (Object) messageLeftOverTracker.getMessageLeftOverFromPreviousPoll()).thenReturn(mList);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<K,V> consumerConfiguration = new ConsumerConfiguration<K,V>(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(5);

		final KafkaStream<K,V> stream = mock(KafkaStream.class);
		final List<KafkaStream<K,V>> streams = new ArrayList<KafkaStream<K,V>>();
		streams.add(stream);
		final Map<String, List<KafkaStream<K,V>>> messageStreams = new HashMap<String, List<KafkaStream<K,V>>>();
		messageStreams.put("topic1", streams);
		when(consumerConfiguration.createMessageStreamsForTopic()).thenReturn(messageStreams);
		final ConsumerIterator<K, V> iterator = mock(ConsumerIterator.class);
		when(stream.iterator()).thenReturn(iterator);
		final MessageAndMetadata<K, V> messageAndMetadata = mock(MessageAndMetadata.class);
		when(iterator.next()).thenReturn(messageAndMetadata);
		when(messageAndMetadata.message()).thenReturn((V) "got message");
		when(messageAndMetadata.topic()).thenReturn("topic1");
		when(messageAndMetadata.partition()).thenReturn(1);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		int sum = 0;

		final Collection<Map<Integer, List<Object>>> values = messages.values();

		for (final Map<Integer, List<Object>> m : values) {
			for (final List<Object> l : m.values()) {
				sum += l.size();
			}

		}
		Assert.assertEquals(5, sum);

		Assert.assertTrue(messages.containsKey("topic1"));
		Assert.assertTrue(messages.containsKey("topic2"));
		Assert.assertTrue(messages.containsKey("topic3"));

		Assert.assertTrue(valueFound(messages.get("topic1").get(1), "value1"));
		Assert.assertTrue(valueFound(messages.get("topic2").get(1), "value2"));
		Assert.assertTrue(valueFound(messages.get("topic3").get(1), "value3"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetConsumerMapWithMessageStreamsWithNullDecoders() {

		final ConsumerMetadata<K,V> mockedConsumerMetadata = mock(ConsumerMetadata.class);

		assertNull(mockedConsumerMetadata.getKeyDecoder());
		assertNull(mockedConsumerMetadata.getValueDecoder());

		final Map<String, Integer> topicsStreamMap = new HashMap<String, Integer>();
		when(mockedConsumerMetadata.getTopicStreamMap()).thenReturn(topicsStreamMap);

		final ConsumerConnectionProvider mockedConsumerConnectionProvider = mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<K,V> mockedMessageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector mockedConsumerConnector = mock(ConsumerConnector.class);

		when(mockedConsumerConnectionProvider.getConsumerConnector()).thenReturn(mockedConsumerConnector);

		final Map<String, List<KafkaStream<K,V>>> messageStreams = new HashMap<String, List<KafkaStream<K,V>>>();
		when((Map<String, List<KafkaStream<K,V>>>)
				(Object) mockedConsumerConnector.createMessageStreams(topicsStreamMap)).thenReturn(messageStreams);

		final ConsumerConfiguration<K,V> consumerConfiguration = new ConsumerConfiguration<K,V>(mockedConsumerMetadata,
				mockedConsumerConnectionProvider, mockedMessageLeftOverTracker);

		consumerConfiguration.createMessageStreamsForTopic();

		verify(mockedConsumerMetadata, atLeast(1)).getTopicStreamMap();
		verify(mockedConsumerConnector, atLeast(1)).createMessageStreams(topicsStreamMap, null, null);
		//verify(mockedConsumerConnector, atMost(0)).createMessageStreams(topicsStreamMap, null, null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetConsumerMapWithMessageStreamsWithDecoders() {

		@SuppressWarnings("unchecked")
		final ConsumerMetadata<String, String> mockedConsumerMetadata = mock(ConsumerMetadata.class);

		final Map<String, Integer> topicsStreamMap = new HashMap<String, Integer>();
		when(mockedConsumerMetadata.getTopicStreamMap()).thenReturn(topicsStreamMap);

		@SuppressWarnings("unchecked")
		final Decoder<String> mockedKeyDecoder = mock(Decoder.class);

		@SuppressWarnings("unchecked")
		final Decoder<String> mockedValueDecoder = mock(Decoder.class);

		when(mockedConsumerMetadata.getKeyDecoder()).thenReturn(mockedKeyDecoder);
		when(mockedConsumerMetadata.getValueDecoder()).thenReturn(mockedValueDecoder);

		final ConsumerConnectionProvider mockedConsumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<String, String> mockedMessageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector mockedConsumerConnector = mock(ConsumerConnector.class);

		when(mockedConsumerConnectionProvider.getConsumerConnector()).thenReturn(mockedConsumerConnector);

		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[],byte[]>>>();
		when(mockedConsumerConnector.createMessageStreams(topicsStreamMap)).thenReturn(messageStreams);

		final ConsumerConfiguration<String, String> consumerConfiguration =
				new ConsumerConfiguration<String, String>(mockedConsumerMetadata, mockedConsumerConnectionProvider,
						mockedMessageLeftOverTracker);

		consumerConfiguration.createMessageStreamsForTopic();

		verify(mockedConsumerMetadata, atLeast(1)).getTopicStreamMap();
		verify(mockedConsumerConnector, atMost(0)).createMessageStreams(topicsStreamMap);
		verify(mockedConsumerConnector, atLeast(1))
				.createMessageStreams(topicsStreamMap, mockedKeyDecoder, mockedValueDecoder);
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForTopicFilterFromSingleStream() {
		final ConsumerMetadata<byte[], String> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<byte[], String> messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerMetadata.getTopicFilterConfiguration()).thenReturn(new TopicFilterConfiguration(".*", 1, false));

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<byte[], String> consumerConfiguration =
				new ConsumerConfiguration<byte[], String>(consumerMetadata, consumerConnectionProvider,
						messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(1);

		final KafkaStream<byte[],String> stream = mock(KafkaStream.class);
		final List<KafkaStream<byte[], String>> streams = new ArrayList<KafkaStream<byte[], String>>();
		streams.add(stream);

		when(consumerConfiguration.createMessageStreamsForTopicFilter()).thenReturn(streams);
		final ConsumerIterator<byte[], String> iterator = mock(ConsumerIterator.class);
		when(stream.iterator()).thenReturn(iterator);
		final MessageAndMetadata<byte[], String> messageAndMetadata = mock(MessageAndMetadata.class);
		when(iterator.next()).thenReturn(messageAndMetadata);
		when(messageAndMetadata.message()).thenReturn("got message");
		when(messageAndMetadata.topic()).thenReturn("topic");
		when(messageAndMetadata.partition()).thenReturn(1);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		Assert.assertEquals(1, messages.size());
		Assert.assertEquals(1, messages.get("topic").size());
		Assert.assertEquals("got message", messages.get("topic").get(1).get(0));

		verify(stream, times(1)).iterator();
		verify(iterator, times(1)).next();
		verify(messageAndMetadata, times(1)).message();
		verify(messageAndMetadata, times(1)).topic();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForTopicFilterFromMultipleStreams() {
		final ConsumerMetadata<byte[], byte[]> consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker<byte[], byte[]> messageLeftOverTracker = mock(MessageLeftOverTracker.class);

		when(consumerMetadata.getTopicFilterConfiguration()).thenReturn(new TopicFilterConfiguration(".*", 1, false));

		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration<byte[], byte[]> consumerConfiguration =
				new ConsumerConfiguration<byte[], byte[]>(consumerMetadata, consumerConnectionProvider,
						messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(3);

		final KafkaStream<byte[], byte[]> stream1 = mock(KafkaStream.class);
		final KafkaStream<byte[], byte[]> stream2 = mock(KafkaStream.class);
		final KafkaStream<byte[], byte[]> stream3 = mock(KafkaStream.class);
		final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
		streams.add(stream1);
		streams.add(stream2);
		streams.add(stream3);

		when(consumerConfiguration.createMessageStreamsForTopicFilter()).thenReturn(streams);
		final ConsumerIterator<byte[], byte[]> iterator1 = mock(ConsumerIterator.class);
		final ConsumerIterator<byte[], byte[]> iterator2 = mock(ConsumerIterator.class);
		final ConsumerIterator<byte[], byte[]> iterator3 = mock(ConsumerIterator.class);

		when(stream1.iterator()).thenReturn(iterator1);
		when(stream2.iterator()).thenReturn(iterator2);
		when(stream3.iterator()).thenReturn(iterator3);
		final MessageAndMetadata<byte[], byte[]> messageAndMetadata1 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<byte[], byte[]> messageAndMetadata2 = mock(MessageAndMetadata.class);
		final MessageAndMetadata<byte[], byte[]> messageAndMetadata3 = mock(MessageAndMetadata.class);

		when(iterator1.next()).thenReturn(messageAndMetadata1);
		when(iterator2.next()).thenReturn(messageAndMetadata2);
		when(iterator3.next()).thenReturn(messageAndMetadata3);

		when(messageAndMetadata1.message()).thenReturn("got message".getBytes());
		when(messageAndMetadata1.topic()).thenReturn("topic");
		when(messageAndMetadata1.partition()).thenReturn(1);

		when(messageAndMetadata2.message()).thenReturn("got message".getBytes());
		when(messageAndMetadata2.topic()).thenReturn("topic");
		when(messageAndMetadata2.partition()).thenReturn(2);

		when(messageAndMetadata3.message()).thenReturn("got message".getBytes());
		when(messageAndMetadata3.topic()).thenReturn("topic");
		when(messageAndMetadata3.partition()).thenReturn(3);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		Assert.assertEquals(1, messages.size());
		int sum = 0;

		final Map<Integer, List<Object>> values = messages.get("topic");

		for (final List<Object> l : values.values()) {
			sum += l.size();
		}

		Assert.assertEquals(3, sum);
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
