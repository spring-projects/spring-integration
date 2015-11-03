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
import static org.springframework.integration.kafka.util.MessageUtils.decodeKey;
import static org.springframework.integration.kafka.util.MessageUtils.decodePayload;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.BrokerAddressListConfiguration;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.rule.KafkaEmbedded;

/**
 * @author Marius Bogoevici
 */

public class NewBrokerRecoveryTests extends AbstractMessageListenerContainerTests {

	@Rule
	public KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(2);

	@Override
	public KafkaEmbedded getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test
	public void testCompleteShutdown() throws Exception {
		int partitionCount = 1;

		createTopic(TEST_TOPIC, partitionCount, 2, 2);

		// stop one Kafka instance - the partition is now underreplicated and only one
		kafkaEmbeddedBrokerRule.bounce(0);

		// connect to the live broker only
		ConnectionFactory connectionFactory =
				new DefaultConnectionFactory(new BrokerAddressListConfiguration(kafkaEmbeddedBrokerRule.getBrokerAddress(1)));
		ArrayList<Partition> readPartitions = new ArrayList<>();
		readPartitions.add(new Partition(TEST_TOPIC, 0));
		final KafkaMessageListenerContainer kafkaMessageListenerContainer =
				new KafkaMessageListenerContainer(connectionFactory,
						readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(1);

		final int expectedMessageCount = 200;
		createMessageSender("none", 1).send(createMessagesInRange(0, 49, TEST_TOPIC, partitionCount));

		final MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData =
				new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(KafkaMessage message) {
				StringDecoder decoder = new StringDecoder(new VerifiableProperties());
				receivedData.put(message.getMetadata().getPartition().getId(),
						new KeyedMessageWithOffset(decodeKey(message, decoder), decodePayload(message, decoder),
								message.getMetadata().getOffset(), Thread.currentThread().getName(),
								message.getMetadata().getPartition().getId()));
				latch.countDown();
			}

		});


		kafkaMessageListenerContainer.start();

		// now start sending messages again
		createMessageSender("none", 1).send(createMessagesInRange(50, 99, TEST_TOPIC, partitionCount));

		// start the other Kafka instance
		kafkaEmbeddedBrokerRule.restart(0);
		// sleep to let the brokers sync up
		kafkaEmbeddedBrokerRule.waitUntilSynced(TEST_TOPIC, 0);
		// bounce the other server
		kafkaEmbeddedBrokerRule.bounce(1);

		// now start sending messages again
		createMessageSender("none", 0).send(createMessagesInRange(100, 149, TEST_TOPIC, partitionCount));

		// stop the other Kafka instance
		kafkaEmbeddedBrokerRule.restart(1);
		// sleep to let the brokers sync up
		kafkaEmbeddedBrokerRule.waitUntilSynced(TEST_TOPIC, 1);

		// bounce the other server
		kafkaEmbeddedBrokerRule.bounce(0);

		// now start sending messages again
		createMessageSender("none", 1).send(createMessagesInRange(149, 199, TEST_TOPIC, partitionCount));


		latch.await(50, TimeUnit.SECONDS);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));

		validateMessageReceipt(receivedData, 1, partitionCount, expectedMessageCount, 1);

	}

}
