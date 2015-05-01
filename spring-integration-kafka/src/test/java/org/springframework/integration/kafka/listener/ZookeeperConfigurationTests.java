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

import org.springframework.integration.kafka.core.Configuration;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.support.ZookeeperConnect;

/**
 * @author Marius Bogoevici
 */
public class ZookeeperConfigurationTests extends AbstractMessageListenerContainerTests {

	@Rule
	public KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(2);

	@Override
	public KafkaEmbedded getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Override
	public Configuration getKafkaConfiguration() {
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect(kafkaEmbeddedBrokerRule.getZookeeperConnectionString());
		return new ZookeeperConfiguration(zookeeperConnect);
	}

	@Test
	public void testLowVolumeLowConcurrency() throws Exception {

		int partitionCount = 5;

		createTopic(TEST_TOPIC, partitionCount, 2, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < partitionCount; i++) {
			readPartitions.add(new Partition(TEST_TOPIC, i));
		}
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(connectionFactory, readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);

		int expectedMessageCount = 100;

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

		createMessageSender("none").send(createMessages(100, TEST_TOPIC, partitionCount));

		latch.await((expectedMessageCount / 5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, 2, partitionCount, expectedMessageCount, 1);

	}


}
