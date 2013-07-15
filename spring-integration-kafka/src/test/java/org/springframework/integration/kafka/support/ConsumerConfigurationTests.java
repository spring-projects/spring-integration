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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author Gunnar Hillert
 * @since 0.5
 */
public class ConsumerConfigurationTests {
	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForSingleTopicFromSingleStream() {
		final ConsumerMetadata consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(1);

		final KafkaStream stream = mock(KafkaStream.class);
		final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
		streams.add(stream);
		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
		messageStreams.put("topic", streams);

		when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
		final ConsumerIterator iterator = mock(ConsumerIterator.class);
		when(stream.iterator()).thenReturn(iterator);
		final MessageAndMetadata messageAndMetadata = mock(MessageAndMetadata.class);
		when(iterator.next()).thenReturn(messageAndMetadata);
		when(messageAndMetadata.message()).thenReturn("got message");
		when(messageAndMetadata.topic()).thenReturn("topic");
		when(messageAndMetadata.partition()).thenReturn(1);

		final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();
		Assert.assertEquals(messages.size(), 1);
		Assert.assertEquals(messages.get("topic").size(), 1);
		Assert.assertEquals(messages.get("topic").get(1).get(0), "got message");

		verify(stream, times(1)).iterator();
		verify(iterator, times(1)).next();
		verify(messageAndMetadata, times(1)).message();
		verify(messageAndMetadata, times(1)).topic();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReceiveMessageForSingleTopicFromMultipleStreams() {
		final ConsumerMetadata consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker messageLeftOverTracker = mock(MessageLeftOverTracker.class);

		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(3);

		final KafkaStream stream1 = mock(KafkaStream.class);
		final KafkaStream stream2 = mock(KafkaStream.class);
		final KafkaStream stream3 = mock(KafkaStream.class);
		final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
		streams.add(stream1);
		streams.add(stream2);
		streams.add(stream3);
		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
		messageStreams.put("topic", streams);

		when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
		final ConsumerIterator iterator1 = mock(ConsumerIterator.class);
		final ConsumerIterator iterator2 = mock(ConsumerIterator.class);
		final ConsumerIterator iterator3 = mock(ConsumerIterator.class);

		when(stream1.iterator()).thenReturn(iterator1);
		when(stream2.iterator()).thenReturn(iterator2);
		when(stream3.iterator()).thenReturn(iterator3);
		final MessageAndMetadata messageAndMetadata1 = mock(MessageAndMetadata.class);
		final MessageAndMetadata messageAndMetadata2 = mock(MessageAndMetadata.class);
		final MessageAndMetadata messageAndMetadata3 = mock(MessageAndMetadata.class);

		when(iterator1.next()).thenReturn(messageAndMetadata1);
		when(iterator2.next()).thenReturn(messageAndMetadata2);
		when(iterator3.next()).thenReturn(messageAndMetadata3);

		when(messageAndMetadata1.message()).thenReturn("got message");
		when(messageAndMetadata1.topic()).thenReturn("topic");
		when(messageAndMetadata1.partition()).thenReturn(1);

		when(messageAndMetadata2.message()).thenReturn("got message");
		when(messageAndMetadata2.topic()).thenReturn("topic");
		when(messageAndMetadata2.partition()).thenReturn(2);

		when(messageAndMetadata3.message()).thenReturn("got message");
		when(messageAndMetadata3.topic()).thenReturn("topic");
		when(messageAndMetadata3.partition()).thenReturn(3);

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
		final ConsumerMetadata consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker messageLeftOverTracker = mock(MessageLeftOverTracker.class);

		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(9);

		final KafkaStream stream1 = mock(KafkaStream.class);
		final KafkaStream stream2 = mock(KafkaStream.class);
		final KafkaStream stream3 = mock(KafkaStream.class);
		final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
		streams.add(stream1);
		streams.add(stream2);
		streams.add(stream3);
		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
		messageStreams.put("topic1", streams);
		messageStreams.put("topic2", streams);
		messageStreams.put("topic3", streams);

		when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
		final ConsumerIterator iterator1 = mock(ConsumerIterator.class);
		final ConsumerIterator iterator2 = mock(ConsumerIterator.class);
		final ConsumerIterator iterator3 = mock(ConsumerIterator.class);

		when(stream1.iterator()).thenReturn(iterator1);
		when(stream2.iterator()).thenReturn(iterator2);
		when(stream3.iterator()).thenReturn(iterator3);
		final MessageAndMetadata messageAndMetadata1 = mock(MessageAndMetadata.class);
		final MessageAndMetadata messageAndMetadata2 = mock(MessageAndMetadata.class);
		final MessageAndMetadata messageAndMetadata3 = mock(MessageAndMetadata.class);

		when(iterator1.next()).thenReturn(messageAndMetadata1);
		when(iterator2.next()).thenReturn(messageAndMetadata2);
		when(iterator3.next()).thenReturn(messageAndMetadata3);

		when(messageAndMetadata1.message()).thenReturn("got message1");
		when(messageAndMetadata1.topic()).thenReturn("topic1");
		when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

		when(messageAndMetadata2.message()).thenReturn("got message2");
		when(messageAndMetadata2.topic()).thenReturn("topic2");
		when(messageAndMetadata1.partition()).thenAnswer(getAnswer());

		when(messageAndMetadata3.message()).thenReturn("got message3");
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
		final ConsumerMetadata consumerMetadata = mock(ConsumerMetadata.class);
		final ConsumerConnectionProvider consumerConnectionProvider =
				mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector consumerConnector = mock(ConsumerConnector.class);

		when(messageLeftOverTracker.getCurrentCount()).thenReturn(3);
		final MessageAndMetadata m1 = new MessageAndMetadata("key1", "value1", "topic1", 1, 1L);
		final MessageAndMetadata m2 = new MessageAndMetadata("key2", "value2", "topic2", 1, 1L);
		final MessageAndMetadata m3 = new MessageAndMetadata("key1", "value3", "topic3", 1, 1L);

		final List<MessageAndMetadata> mList = new ArrayList<MessageAndMetadata>();
		mList.add(m1);
		mList.add(m2);
		mList.add(m3);

		when(messageLeftOverTracker.getMessageLeftOverFromPreviousPoll()).thenReturn(mList);

		when(consumerConnectionProvider.getConsumerConnector()).thenReturn(consumerConnector);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		consumerConfiguration.setMaxMessages(5);

		final KafkaStream stream = mock(KafkaStream.class);
		final List<KafkaStream<byte[], byte[]>> streams = new ArrayList<KafkaStream<byte[], byte[]>>();
		streams.add(stream);
		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[], byte[]>>>();
		messageStreams.put("topic1", streams);

		when(consumerConfiguration.getConsumerMapWithMessageStreams()).thenReturn(messageStreams);
		final ConsumerIterator<String, String> iterator = mock(ConsumerIterator.class);
		when(stream.iterator()).thenReturn(iterator);
		final MessageAndMetadata<String, String> messageAndMetadata = mock(MessageAndMetadata.class);
		when(iterator.next()).thenReturn(messageAndMetadata);
		when(messageAndMetadata.message()).thenReturn("got message");
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
		Assert.assertEquals(sum, 5);

		Assert.assertTrue(messages.containsKey("topic1"));
		Assert.assertTrue(messages.containsKey("topic2"));
		Assert.assertTrue(messages.containsKey("topic3"));

		Assert.assertTrue(valueFound(messages.get("topic1").get(1), "value1"));
		Assert.assertTrue(valueFound(messages.get("topic2").get(1), "value2"));
		Assert.assertTrue(valueFound(messages.get("topic3").get(1), "value3"));
	}

	@Test
	public void testGetConsumerMapWithMessageStreamsWithNullDecoders() {

		final ConsumerMetadata<?,?> mockedConsumerMetadata = mock(ConsumerMetadata.class);

		assertNull(mockedConsumerMetadata.getKeyDecoder());
		assertNull(mockedConsumerMetadata.getValueDecoder());

		final Map<String, Integer> topicsStreamMap = new HashMap<String, Integer>();
		when(mockedConsumerMetadata.getTopicStreamMap()).thenReturn(topicsStreamMap);

		final ConsumerConnectionProvider mockedConsumerConnectionProvider = mock(ConsumerConnectionProvider.class);
		final MessageLeftOverTracker mockedMessageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector mockedConsumerConnector = mock(ConsumerConnector.class);

		when(mockedConsumerConnectionProvider.getConsumerConnector()).thenReturn(mockedConsumerConnector);

		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[],byte[]>>>();
		when(mockedConsumerConnector.createMessageStreams(topicsStreamMap)).thenReturn(messageStreams);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(mockedConsumerMetadata,
				mockedConsumerConnectionProvider, mockedMessageLeftOverTracker);

		consumerConfiguration.getConsumerMapWithMessageStreams();

		verify(mockedConsumerMetadata, atLeast(1)).getTopicStreamMap();
		verify(mockedConsumerConnector, atLeast(1)).createMessageStreams(topicsStreamMap);
		verify(mockedConsumerConnector, atMost(0)).createMessageStreams(topicsStreamMap, null, null);
	}

	@Test
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
		final MessageLeftOverTracker mockedMessageLeftOverTracker = mock(MessageLeftOverTracker.class);
		final ConsumerConnector mockedConsumerConnector = mock(ConsumerConnector.class);

		when(mockedConsumerConnectionProvider.getConsumerConnector()).thenReturn(mockedConsumerConnector);

		final Map<String, List<KafkaStream<byte[], byte[]>>> messageStreams = new HashMap<String, List<KafkaStream<byte[],byte[]>>>();
		when(mockedConsumerConnector.createMessageStreams(topicsStreamMap)).thenReturn(messageStreams);

		final ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(mockedConsumerMetadata,
				mockedConsumerConnectionProvider, mockedMessageLeftOverTracker);

		consumerConfiguration.getConsumerMapWithMessageStreams();

		verify(mockedConsumerMetadata, atLeast(1)).getTopicStreamMap();
		verify(mockedConsumerConnector, atMost(0)).createMessageStreams(topicsStreamMap);
		verify(mockedConsumerConnector, atLeast(1)).createMessageStreams(topicsStreamMap, mockedKeyDecoder, mockedValueDecoder);
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
