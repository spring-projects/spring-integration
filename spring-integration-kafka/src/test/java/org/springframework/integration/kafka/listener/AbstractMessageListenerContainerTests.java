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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.kafka.util.MessageUtils.decodeKey;
import static org.springframework.integration.kafka.util.MessageUtils.decodePayload;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.bag.MutableBag;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.block.procedure.Procedure;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.map.MutableMap;
import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.factory.Sets;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;
import com.gs.collections.impl.tuple.Tuples;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;
import org.hamcrest.Matchers;

import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.KafkaMessage;

/**
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageListenerContainerTests extends AbstractBrokerTests {

	public void runMessageListenerTest(int maxReceiveSize, int concurrency, int partitionCount, int testMessageCount, int divisionFactor, int compressionCodec, String topic) throws Exception {

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < partitionCount; i++) {
			if(i % divisionFactor == 0) {
				readPartitions.add(new Partition(topic, i));
			}
		}
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(connectionFactory, readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(maxReceiveSize);
		kafkaMessageListenerContainer.setConcurrency(concurrency);

		int expectedMessageCount = testMessageCount / divisionFactor;

		final MutableListMultimap<Integer,KeyedMessageWithOffset> receivedData = new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(KafkaMessage message) {
				StringDecoder decoder = new StringDecoder(new VerifiableProperties());
				receivedData.put(message.getMetadata().getPartition().getId(),new KeyedMessageWithOffset(decodeKey(message, decoder), decodePayload(message, decoder), message.getMetadata().getOffset(), Thread.currentThread().getName(), message.getMetadata().getPartition().getId()));
				latch.countDown();
			}
		});

		kafkaMessageListenerContainer.start();

		createStringProducer(compressionCodec).send(createMessages(testMessageCount, topic));

		latch.await((expectedMessageCount/5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, concurrency, partitionCount, testMessageCount, expectedMessageCount, readPartitions, divisionFactor);

	}

	@SuppressWarnings("serial")
	public void validateMessageReceipt(MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData, int concurrency, int partitionCount, int testMessageCount, int expectedMessageCount, ArrayList<Partition> readPartitions, int divisionFactor) {
		// Group messages received by processing thread
		MutableListMultimap<String, KeyedMessageWithOffset> messagesByThread = receivedData.valuesView().toList().groupBy(new Function<KeyedMessageWithOffset, String>() {
			@Override
			public String valueOf(KeyedMessageWithOffset object) {
				return object.getThreadName();
			}
		});

		// Execution has taken place on as many distinct threads as configured
		assertThat(messagesByThread.keysView().size(), Matchers.equalTo(concurrency));

		// Group partitions by thread
		MutableMap<String, MutableSet<Integer>> partitionsByThread = messagesByThread.toMap().collect(new Function2<String, RichIterable<KeyedMessageWithOffset>, Pair<String, MutableSet<Integer>>>() {
			@Override
			public Pair<String, MutableSet<Integer>> value(String argument1, RichIterable<KeyedMessageWithOffset> argument2) {
				return Tuples.pair(argument1, argument2.collect(new Function<KeyedMessageWithOffset, Integer>() {
					@Override
					public Integer valueOf(KeyedMessageWithOffset object) {
						return object.getPartition();
					}
				}).toSet());
			}
		});

		// Messages from a partition have been executed on the same thread and groups are mutually exclusive
		final MutableSet<Integer> validatedPartitions = Sets.mutable.of();
		partitionsByThread.valuesView().forEach(new Procedure<MutableSet<Integer>>() {
			@Override
			public void value(MutableSet<Integer> partitions) {
				assertThat(validatedPartitions.intersect(partitions), empty());
				validatedPartitions.addAll(partitions);
			}
		});

		// All partitions are accounted for, but only the ones that we were expecting to read from
		for (int i = 0; i < partitionCount; i++) {
			if (i % divisionFactor == 0) {
				assertThat(validatedPartitions, hasItem(i));
			} else {
				assertThat(validatedPartitions, not(hasItem(i)));
			}
		}

		// Sort data by payload in order to identify duplicates
		MutableList<String> sortedPayloads = receivedData.valuesView().toList().collect(new Function<KeyedMessageWithOffset, String>() {
			@Override
			public String valueOf(KeyedMessageWithOffset object) {
				return object.getPayload();
			}
		}).sortThis();

		// Remove unique values - what is left are duplicates
		MutableBag<String> duplicates = sortedPayloads.toBag();
		duplicates.removeAll(sortedPayloads.toSet());

		// The final set has exactly the same size as the message count
		assertThat(sortedPayloads, hasSize(expectedMessageCount));

		// There are no duplicates - all messages have been received only once
		assertThat(duplicates, hasSize(0));

		// Group offsets by partition
		MutableMap<Integer, MutableList<Long>> offsetsByPartition = receivedData.toMap().collect(new Function2<Integer, RichIterable<KeyedMessageWithOffset>, Pair<Integer, MutableList<Long>>>() {
			@Override
			public Pair<Integer, MutableList<Long>> value(Integer partition, RichIterable<KeyedMessageWithOffset> argument2) {
				return Tuples.pair(partition, argument2.collect(new Function<KeyedMessageWithOffset, Long>() {
					@Override
					public Long valueOf(KeyedMessageWithOffset object) {
						return object.getOffset();
					}
				}).toList());
			}
		});

		// Check that the sequence of offsets has been processed in ascending order, with no gaps
		for (MutableList<Long> offsetsForPartition : offsetsByPartition.valuesView()) {
			for (int i = 0; i < offsetsForPartition.size() - 1; i++) {
				assertThat(offsetsForPartition.get(i+1), equalTo(offsetsForPartition.get(i) + 1));
			}
		}
	}

	static class KeyedMessageWithOffset {

		String key;

		String payload;

		Long offset;

		String threadName;

		int partition;

		public KeyedMessageWithOffset(String key, String payload, Long offset, String threadName, int partition) {
			this.key = key;
			this.payload = payload;
			this.offset = offset;
			this.threadName = threadName;
			this.partition = partition;
		}

		public String getKey() {
			return key;
		}

		public String getPayload() {
			return payload;
		}

		public Long getOffset() {
			return offset;
		}

		public String getThreadName() {
			return threadName;
		}

		public int getPartition() {
			return partition;
		}
	}
}
