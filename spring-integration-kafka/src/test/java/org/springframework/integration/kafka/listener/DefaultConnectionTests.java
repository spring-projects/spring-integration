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

import kafka.producer.Producer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.Connection;
import org.springframework.integration.kafka.core.DefaultConnection;
import org.springframework.integration.kafka.core.FetchRequest;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageBatch;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.util.MessageUtils;

/**
 * @author Marius Bogoevici
 */
public class DefaultConnectionTests extends AbstractBrokerTests {

	@Rule
	public KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaEmbedded getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test
	public void testFetchPartitionMetadata() throws Exception {
		createTopic(TEST_TOPIC, 1, 1, 1);
		Connection brokerConnection =
				new DefaultConnection(getKafkaRule().getBrokerAddresses().get(0), "client", 64*1024, 3000, 1, 10000);
		Partition partition = new Partition(TEST_TOPIC, 0);
		Result<Long> result = brokerConnection.fetchInitialOffset(-1, partition);
		Assert.assertEquals(0, result.getErrors().size());
		Assert.assertEquals(1, result.getResults().size());
		Assert.assertEquals(Long.valueOf(0), result.getResults().get(partition));
	}

	@Test
	public void testReceiveMessages() throws Exception {
		createTopic(TEST_TOPIC, 1, 1, 1);
		Producer<String, String> producer = createStringProducer(0);
		producer.send( createMessages(10, TEST_TOPIC));
		Connection brokerConnection =
				new DefaultConnection(getKafkaRule().getBrokerAddresses().get(0), "client", 64*1024, 3000, 1, 10000);
		Partition partition = new Partition(TEST_TOPIC, 0);
		FetchRequest fetchRequest = new FetchRequest(partition, 0L, 1000);
		Result<KafkaMessageBatch> result = brokerConnection.fetch(fetchRequest);
		Assert.assertEquals(0, result.getErrors().size());
		Assert.assertEquals(1, result.getResults().size());
		Assert.assertEquals(10, result.getResults().get(partition).getMessages().size());
		Assert.assertEquals(10,result.getResults().get(partition).getHighWatermark());
		StringDecoder decoder = new StringDecoder();
		int i = 0;
		for (KafkaMessage kafkaMessage : result.getResults().get(partition).getMessages()) {
			Assert.assertEquals("Key " + i, MessageUtils.decodeKey(kafkaMessage, decoder));
			Assert.assertEquals("Message " + i, MessageUtils.decodePayload(kafkaMessage, decoder));
			i++;
		}
	}

	@Test
	public void testReceiveMessagesWithGZipCompression() throws Exception {
		createTopic(TEST_TOPIC, 1, 1, 1);
		Producer<String, String> producer = createStringProducer(1);
		producer.send( createMessages(10, TEST_TOPIC));
		Connection brokerConnection =
				new DefaultConnection(getKafkaRule().getBrokerAddresses().get(0), "client", 64*1024, 3000, 1, 10000);
		Partition partition = new Partition(TEST_TOPIC, 0);
		FetchRequest fetchRequest = new FetchRequest(partition, 0L, 1000);
		Result<KafkaMessageBatch> result = brokerConnection.fetch(fetchRequest);
		Assert.assertEquals(0, result.getErrors().size());
		Assert.assertEquals(1, result.getResults().size());
		Assert.assertEquals(10, result.getResults().get(partition).getMessages().size());
		Assert.assertEquals(10,result.getResults().get(partition).getHighWatermark());
		StringDecoder decoder = new StringDecoder();
		int i = 0;
		for (KafkaMessage kafkaMessage : result.getResults().get(partition).getMessages()) {
			Assert.assertEquals("Key " + i, MessageUtils.decodeKey(kafkaMessage, decoder));
			Assert.assertEquals("Message " + i, MessageUtils.decodePayload(kafkaMessage, decoder));
			i++;
		}
	}

	@Test
	@Ignore
	/**
	 * The compression codec '2' is for Snappy:
	 * {@code producerConfig.put("compression.codec",  2);}
	 * Since it relies on the native library we can't test it on all environment,
	 * because we may not have permission to load dll(so).
	 */
	public void testReceiveMessagesWithSnappyCompression() throws Exception {
		createTopic(TEST_TOPIC, 1, 1, 1);
		Producer<String, String> producer = createStringProducer(2);
		producer.send( createMessages(10, TEST_TOPIC));
		Connection brokerConnection =
				new DefaultConnection(getKafkaRule().getBrokerAddresses().get(0), "client", 64*1024, 3000, 1, 10000);
		Partition partition = new Partition(TEST_TOPIC, 0);
		FetchRequest fetchRequest = new FetchRequest(partition, 0L, 1000);
		Result<KafkaMessageBatch> result = brokerConnection.fetch(fetchRequest);
		Assert.assertEquals(0, result.getErrors().size());
		Assert.assertEquals(1, result.getResults().size());
		Assert.assertEquals(10, result.getResults().get(partition).getMessages().size());
		Assert.assertEquals(10,result.getResults().get(partition).getHighWatermark());
		StringDecoder decoder = new StringDecoder();
		int i = 0;
		for (KafkaMessage kafkaMessage : result.getResults().get(partition).getMessages()) {
			Assert.assertEquals("Key " + i, MessageUtils.decodeKey(kafkaMessage, decoder));
			Assert.assertEquals("Message " + i, MessageUtils.decodePayload(kafkaMessage, decoder));
			i++;
		}
	}

}
