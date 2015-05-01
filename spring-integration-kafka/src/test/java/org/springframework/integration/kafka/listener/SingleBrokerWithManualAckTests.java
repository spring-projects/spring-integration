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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.kafka.util.MessageUtils.decodeKey;
import static org.springframework.integration.kafka.util.MessageUtils.decodePayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.metadata.SimpleMetadataStore;

import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;

import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

/**
 * @author Marius Bogoevici
 */
public class SingleBrokerWithManualAckTests extends AbstractMessageListenerContainerTests {

	@Rule
	public final KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaEmbedded getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMessageListenerRequiredIfAutoAckFail() throws Exception {
		createTopic(TEST_TOPIC, 5, 1, 1);
		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < 5; i++) {
			readPartitions.add(new Partition(TEST_TOPIC, i));
		}
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(
				connectionFactory, readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);

		kafkaMessageListenerContainer.setMessageListener(new Object());
	}

	@Test
	public void testLowVolumeLowConcurrency() throws Exception {
		int partitionCount = 5;

		createTopic(TEST_TOPIC, partitionCount, 1, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < partitionCount; i++) {
			if (i % 1 == 0) {
				readPartitions.add(new Partition(TEST_TOPIC, i));
			}
		}
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(
				connectionFactory, readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);
		MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(connectionFactory);
		SimpleMetadataStore metadataStore = new SimpleMetadataStore();
		offsetManager.setMetadataStore(metadataStore);
		kafkaMessageListenerContainer.setOffsetManager(offsetManager);

		int expectedMessageCount = 100;

		final List<Acknowledgment> acknowledgments = Collections.synchronizedList(new ArrayList<Acknowledgment>());

		final MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData =
				new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new AcknowledgingMessageListener() {
			@Override
			public void onMessage(KafkaMessage message, Acknowledgment acknowledgment) {
				StringDecoder decoder = new StringDecoder(new VerifiableProperties());
				receivedData.put(message.getMetadata().getPartition().getId(),
						new KeyedMessageWithOffset(decodeKey(message, decoder), decodePayload(message, decoder),
								message.getMetadata().getOffset(), Thread.currentThread().getName(), message
										.getMetadata().getPartition().getId()));
				acknowledgments.add(acknowledgment);
				latch.countDown();
			}
		});

		kafkaMessageListenerContainer.start();

		createMessageSender("none").send(createMessages(100, TEST_TOPIC, partitionCount));

		latch.await((expectedMessageCount / 5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, 2, partitionCount, expectedMessageCount, 1);

		// at this point, all messages have been processed but not acknowledged
		for (Partition readPartition : readPartitions) {
			assertThat(metadataStore.get(offsetManager.generateKey(readPartition)), nullValue());
		}

		// now we did acknowledge them in the reverse order. This way we check that only
		// the highest value was acknowledged
		for (Acknowledgment acknowledgment : FastList.newList(acknowledgments).reverseThis()) {
			acknowledgment.acknowledge();
		}

		// now they are all acknowledged
		for (Partition readPartition : readPartitions) {
			assertThat(metadataStore.get(offsetManager.generateKey(readPartition)), equalTo(String.valueOf(20)));
		}
	}

}
