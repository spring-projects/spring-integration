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

package org.springframework.integration.kafka.offset;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import kafka.api.OffsetRequest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.PartitionNotFoundException;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
import org.springframework.integration.kafka.util.TopicUtils;

/**
 * @author Marius Bogoevici
 */
public abstract class AbstractOffsetManagerTests {

	@Rule
	public KafkaRule kafkaRule = new KafkaEmbedded(1);

	private String TEST_TOPIC = "si_test_topic";

	@Test(expected = PartitionNotFoundException.class)
	public void testFailureWhenPartitionDoesNotExist() throws Exception {
		OffsetManager offsetManager = createOffsetManager(OffsetRequest.EarliestTime(), "offset");
		offsetManager.getOffset(new Partition(TEST_TOPIC, 1));
	}

	@Test
	public void testInitialization() throws Exception {

		int numPartitions = 3;

		TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TEST_TOPIC, numPartitions, 1);

		OffsetManager offsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");

		Partition[] partitions = new Partition[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			partitions[i] = new Partition(TEST_TOPIC, i);
		}
		// Earliest
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		// send data to increase offsets on topics
		Producer<String, String> producer = createProducer();
		for (int i = 0; i < 10; i++) {
			producer.send(new ProducerRecord<String, String>(TEST_TOPIC, i%3, String.valueOf(i), String.valueOf(i))).get();
		}

		// earliest time resets at the start of the queue
		OffsetManager offsetManager2 = createOffsetManager(OffsetRequest.EarliestTime(), "offset2");
		assertThat(offsetManager2.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager2.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager2.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.close();

		// latest time resets at the end of the queue
		OffsetManager offsetManager3 = createOffsetManager(OffsetRequest.LatestTime(), "offset3");
		assertThat(offsetManager3.getOffset(partitions[0]), equalTo(4L));
		assertThat(offsetManager3.getOffset(partitions[1]), equalTo(3L));
		assertThat(offsetManager3.getOffset(partitions[2]), equalTo(3L));

	}

	@Test
	public void testUpdate() throws Exception {
		int numPartitions = 3;

		TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TEST_TOPIC, numPartitions, 1);

		OffsetManager offsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");

		Partition[] partitions = new Partition[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			partitions[i] = new Partition(TEST_TOPIC, i);
		}
		// Earliest
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		// send data to increase offsets on topics
		Producer<String, String> producer = createProducer();
		for (int i = 0; i < 10; i++) {
			producer.send(new ProducerRecord<String, String>(TEST_TOPIC,
					i%numPartitions, String.valueOf(i), String.valueOf(i))).get();
		}

		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.updateOffset(partitions[0], 5L);
		offsetManager1.updateOffset(partitions[1], 4L);
		offsetManager1.updateOffset(partitions[2], 3L);
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(5L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(4L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(3L));


		// a new offset manager with the same consumerId will observe the updates
		OffsetManager newOffsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");
		assertThat(newOffsetManager1.getOffset(partitions[0]), equalTo(5L));
		assertThat(newOffsetManager1.getOffset(partitions[1]), equalTo(4L));
		assertThat(newOffsetManager1.getOffset(partitions[2]), equalTo(3L));

		offsetManager1.close();

		// a new offset manager with a different consumerId will not observe the updates
		OffsetManager offsetManager2 = createOffsetManager(OffsetRequest.EarliestTime(), "offset2");
		assertThat(offsetManager2.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager2.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager2.getOffset(partitions[2]), equalTo(0L));

	}

	@Test
	public void testRemove() throws Exception {
		int numPartitions = 3;

		TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TEST_TOPIC, numPartitions, 1);

		OffsetManager offsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");

		Partition[] partitions = new Partition[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			partitions[i] = new Partition(TEST_TOPIC, i);
		}
		// Earliest
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		// send data to increase offsets on topics
		Producer<String, String> producer = createProducer();
		for (int i = 0; i < 6; i++) {
			producer.send(new ProducerRecord<String, String>(TEST_TOPIC, i%numPartitions, String.valueOf(i), String.valueOf(i))).get();
		}

		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.updateOffset(partitions[0], 5L);
		offsetManager1.updateOffset(partitions[1], 4L);
		offsetManager1.updateOffset(partitions[2], 3L);

		offsetManager1.deleteOffset(partitions[1]);
		offsetManager1.deleteOffset(partitions[2]);

		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(5L));
		// partition 1,2 will be reset
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		// a new offset manager with the same consumerId will observe the updates
		OffsetManager newOffsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");
		assertThat(newOffsetManager1.getOffset(partitions[0]), equalTo(5L));
		assertThat(newOffsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(newOffsetManager1.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.close();

		// a new offset manager with a different consumerId will not observe the updates
		OffsetManager offsetManager2 = createOffsetManager(OffsetRequest.LatestTime(), "offset2");
		assertThat(offsetManager2.getOffset(partitions[0]), equalTo(2L));
		assertThat(offsetManager2.getOffset(partitions[1]), equalTo(2L));
		assertThat(offsetManager2.getOffset(partitions[2]), equalTo(2L));
	}

	@Test
	public void testInitialOffsets() throws Exception {
		int numPartitions = 3;

		TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TEST_TOPIC, numPartitions, 1);

		Partition[] partitions = new Partition[numPartitions];
		for (int i = 0; i < numPartitions; i++) {
			partitions[i] = new Partition(TEST_TOPIC, i);
		}

		HashMap<Partition, Long> initialOffsets = new HashMap<Partition, Long>();
		initialOffsets.put(partitions[0], 8L);
		initialOffsets.put(partitions[1], 9L);
		initialOffsets.put(partitions[2], 10L);



		OffsetManager offsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1", initialOffsets);

		// send data to increase offsets on topics
		Producer<String, String> producer = createProducer();
		for (int i = 0; i < 6; i++) {
			producer.send(new ProducerRecord<String, String>(TEST_TOPIC, i%numPartitions, String.valueOf(i), String.valueOf(i))).get();
		}

		// The offset manager starts at the configured offsets
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(8L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(9L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(10L));

		// a new offset manager with the same consumerId will not observe any updates yet
		OffsetManager newOffsetManager1 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");
		assertThat(newOffsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(newOffsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(newOffsetManager1.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.updateOffset(partitions[0], 9L);
		offsetManager1.updateOffset(partitions[1], 10L);
		offsetManager1.updateOffset(partitions[2], 11L);

		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(9L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(10L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(11L));

		// a new offset manager with the same consumerId will observe the updates
		OffsetManager newOffsetManager2 = createOffsetManager(OffsetRequest.EarliestTime(), "offset1");
		assertThat(newOffsetManager2.getOffset(partitions[0]), equalTo(9L));
		assertThat(newOffsetManager2.getOffset(partitions[1]), equalTo(10L));
		assertThat(newOffsetManager2.getOffset(partitions[2]), equalTo(11L));

		offsetManager1.deleteOffset(partitions[0]);
		offsetManager1.deleteOffset(partitions[1]);

		// after delete, the offset manager starts in an initial position
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(8L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(9L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(11L));

		offsetManager1.resetOffsets(Arrays.asList(partitions));
		// after reset, the offset manager starts at the reference timestamp
		assertThat(offsetManager1.getOffset(partitions[0]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[1]), equalTo(0L));
		assertThat(offsetManager1.getOffset(partitions[2]), equalTo(0L));

		offsetManager1.close();

		// a new offset manager with a different consumerId will not observe the updates
		OffsetManager offsetManager2 = createOffsetManager(OffsetRequest.LatestTime(), "offset2");
		assertThat(offsetManager2.getOffset(partitions[0]), equalTo(2L));
		assertThat(offsetManager2.getOffset(partitions[1]), equalTo(2L));
		assertThat(offsetManager2.getOffset(partitions[2]), equalTo(2L));
	}

	protected DefaultConnectionFactory createConnectionFactory() throws Exception {
		DefaultConnectionFactory connectionFactory
				= new DefaultConnectionFactory(new ZookeeperConfiguration(kafkaRule.getZookeeperConnectionString()));
		connectionFactory.afterPropertiesSet();
		return connectionFactory;
	}

	protected Producer<String, String> createProducer() {
		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", kafkaRule.getBrokersAsString());
		EncoderAdaptingSerializer<String> serializer = new EncoderAdaptingSerializer<>(new StringEncoder());
		return new KafkaProducer<String, String>(properties, serializer, serializer);
		}

	protected OffsetManager createOffsetManager(long referenceTimestamp, String consumerId) throws Exception {
		return createOffsetManager(referenceTimestamp, consumerId, new HashMap<Partition, Long>());
	}

	protected abstract OffsetManager createOffsetManager(long referenceTimestamp,
			String consumerId, Map<Partition, Long> initialOffsets) throws Exception;

}
