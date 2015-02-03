/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;
import com.gs.collections.impl.utility.Iterate;

import kafka.message.NoCompressionCodec$;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 */
public class KafkaMessageDrivenChannelAdapterWithWrongOffsetTests extends AbstractMessageListenerContainerTests {

	@Rule
	public final KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaRule getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test
	@SuppressWarnings("serial")
	public void testLowVolumeLowConcurrencyResetAtEarliest() throws Exception {

		// we will send 300 messages: first 200, then another 100
		// we will start reading from all partitions at offset 100
		int expectedMessageCount = 200;

		createTopic(TEST_TOPIC, 5, 1, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		Map<Partition, Long> startingOffsets = new HashMap<Partition, Long>();
		for (int i = 0; i < 5; i++) {
			Partition partition = new Partition(TEST_TOPIC, i);
			readPartitions.add(partition);
			startingOffsets.put(partition, 900L);
		}

		final KafkaMessageListenerContainer kafkaMessageListenerContainer =
				new KafkaMessageListenerContainer(connectionFactory,
						readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);
		MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(connectionFactory, startingOffsets);
		kafkaMessageListenerContainer.setOffsetManager(offsetManager);

		// we send 100 messages
		createStringProducer(NoCompressionCodec$.MODULE$.codec()).send(createMessagesInRange(0, 199, TEST_TOPIC));

		final MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData =
				new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);

		KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter(kafkaMessageListenerContainer);

		StringDecoder decoder = new StringDecoder();
		kafkaMessageDrivenChannelAdapter.setKeyDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setPayloadDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setBeanFactory(mock(BeanFactory.class));

		kafkaMessageDrivenChannelAdapter.setOutputChannel(new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				boolean addedSuccessfully = receivedData.put(
						(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
						new KeyedMessageWithOffset(
								(String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
								(String) message.getPayload(),
								(Long) message.getHeaders().get(KafkaHeaders.OFFSET),
								Thread.currentThread().getName(),
								(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID)));
				latch.countDown();
				return addedSuccessfully;
			}


			@Override
			public boolean send(Message<?> message, long timeout) {
				return send(message);
			}
		});

		kafkaMessageDrivenChannelAdapter.afterPropertiesSet();
		kafkaMessageDrivenChannelAdapter.start();

		createStringProducer(NoCompressionCodec$.MODULE$.codec()).send(createMessagesInRange(200, 299, TEST_TOPIC));

		latch.await((expectedMessageCount / 5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, 2, 5, 100, expectedMessageCount, readPartitions, 1);

		// For all received messages
		Collection<KeyedMessageWithOffset> allReceivedMessages =
				Iterate.flatCollect(receivedData.keyMultiValuePairsView(),
						new Function<Pair<Integer, RichIterable<KeyedMessageWithOffset>>, RichIterable<KeyedMessageWithOffset>>() {

							@Override
							public RichIterable<KeyedMessageWithOffset> valueOf(Pair<Integer, RichIterable<KeyedMessageWithOffset>> object) {
								return object.getTwo();
							}

						});

		// We extract the sequence value, i.e. "Message xx"
		Integer minValueInMessage = FastList.newList(allReceivedMessages)
				.collect(new Function<KeyedMessageWithOffset, Integer>() {
					@Override
					public Integer valueOf(KeyedMessageWithOffset object) {
						return Integer.parseInt(object.getPayload().split(" ")[1]);
					}
				}).min();

		// The lowest received value is 0. That is correct, because reading started at 0 after the reset
		assertThat(minValueInMessage, equalTo(0));
	}

	@Test
	@SuppressWarnings("serial")
	public void testLowVolumeLowConcurrencyResetAtLatest() throws Exception {

		// we will send 300 messages: first 200, then another 100
		// we will start reading from all partitions at offset 900, but since that is reset
		int expectedMessageCount = 100;

		createTopic(TEST_TOPIC, 5, 1, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		Map<Partition, Long> startingOffsets = new HashMap<Partition, Long>();
		for (int i = 0; i < 5; i++) {
			Partition partition = new Partition(TEST_TOPIC, i);
			readPartitions.add(partition);
			startingOffsets.put(partition, 900L);
		}

		final KafkaMessageListenerContainer kafkaMessageListenerContainer =
				new KafkaMessageListenerContainer(connectionFactory,
						readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);
		final MetadataStoreOffsetManager offsetManager =
				new MetadataStoreOffsetManager(connectionFactory, startingOffsets);
		offsetManager.setReferenceTimestamp(-1);
		final CountDownLatch offsetResetLatch = new CountDownLatch(1);

		// Manually generated proxy for catching the reset event. We only want to send data once we are
		// sure that the OffsetManager has been reset. Otherwise the test may not finish (due to pollers starting after send

		OffsetManager offsetManagerWrapper = new OffsetManager() {

			@Override
			public void flush() throws IOException {
				offsetManager.flush();
			}

			@Override
			public void close() throws IOException {
				offsetManager.close();
			}

			@Override
			public void deleteOffset(Partition partition) {
				offsetManager.deleteOffset(partition);
			}

			@Override
			public void resetOffsets(Collection<Partition> partitionsToReset) {
				System.out.println("resetting " + offsetResetLatch.getCount() + " partitions: "
						+ StringUtils.collectionToDelimitedString(partitionsToReset, ","));
				offsetManager.resetOffsets(partitionsToReset);
				offsetResetLatch.countDown();
			}

			@Override
			public long getOffset(Partition partition) {
				long offset = offsetManager.getOffset(partition);
				System.out.println("Result for " + partition + " offset: " + offset);
				return offset;
			}

			@Override
			public void updateOffset(Partition partition, long offset) {
				offsetManager.updateOffset(partition, offset);
			}

		};

		kafkaMessageListenerContainer.setOffsetManager(offsetManagerWrapper);

		createStringProducer(NoCompressionCodec$.MODULE$.codec()).send(createMessagesInRange(0, 199, TEST_TOPIC));

		final MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData =
				new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);

		KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter(kafkaMessageListenerContainer);

		StringDecoder decoder = new StringDecoder();
		kafkaMessageDrivenChannelAdapter.setKeyDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setPayloadDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		kafkaMessageDrivenChannelAdapter.setOutputChannel(new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				boolean addedSuccessfully = receivedData.put(
						(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
						new KeyedMessageWithOffset(
								(String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
								(String) message.getPayload(),
								(Long) message.getHeaders().get(KafkaHeaders.OFFSET),
								Thread.currentThread().getName(),
								(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID)));
				latch.countDown();
				return addedSuccessfully;
			}

			@Override
			public boolean send(Message<?> message, long timeout) {
				return send(message);
			}
		});

		kafkaMessageDrivenChannelAdapter.afterPropertiesSet();
		kafkaMessageDrivenChannelAdapter.start();

		// we wait for the reset event first
		offsetResetLatch.await(1000, TimeUnit.MILLISECONDS);
		Thread.sleep(1000);
		createStringProducer(NoCompressionCodec$.MODULE$.codec()).send(createMessagesInRange(200, 299, TEST_TOPIC));

		latch.await((expectedMessageCount / 5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, 2, 5, 100, expectedMessageCount, readPartitions, 1);

		// For all received messages
		Collection<KeyedMessageWithOffset> allReceivedMessages =
				Iterate.flatCollect(receivedData.keyMultiValuePairsView(),
						new Function<Pair<Integer, RichIterable<KeyedMessageWithOffset>>, RichIterable<KeyedMessageWithOffset>>() {

							@Override
							public RichIterable<KeyedMessageWithOffset> valueOf(Pair<Integer, RichIterable<KeyedMessageWithOffset>> object) {
								return object.getTwo();
							}

						});

		// We extract the sequence value, i.e. "Message xx"
		Integer minValueInMessage = FastList.newList(allReceivedMessages)
				.collect(new Function<KeyedMessageWithOffset, Integer>() {

					@Override
					public Integer valueOf(KeyedMessageWithOffset object) {
						return Integer.parseInt(object.getPayload().split(" ")[1]);
					}

				})
				.min();

		// The lowest received value is 0. That is correct, because reading started at 0 after the reset
		assertThat(minValueInMessage, equalTo(200));
	}

}
