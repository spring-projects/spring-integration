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

import static org.springframework.integration.kafka.util.TopicUtils.ensureTopicCreated;
import static scala.collection.JavaConversions.asScalaBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.multimap.Multimap;
import com.gs.collections.api.multimap.MutableMultimap;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.factory.Multimaps;
import com.gs.collections.impl.tuple.Tuples;
import kafka.admin.AdminUtils;
import kafka.utils.TestUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import scala.collection.JavaConversions;
import scala.collection.Map;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Seq;

import org.springframework.integration.kafka.core.BrokerAddressListConfiguration;
import org.springframework.integration.kafka.core.Configuration;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;

/**
 * @author Marius Bogoevici
 */
public abstract class AbstractBrokerTests {

	private static final Log log = LogFactory.getLog(AbstractBrokerTests.class);

	public static final String TEST_TOPIC = "test-topic";

	public abstract KafkaRule getKafkaRule();

	@After
	public void cleanUp() {
		deleteTopic(TEST_TOPIC);
	}

	public void createTopic(String topicName, int partitionCount, int brokers, int replication) {
		createTopic(getKafkaRule().getZkClient(), topicName, partitionCount, brokers, replication);
	}

	@SuppressWarnings("unchecked")
	public void createTopic(ZkClient zkClient, String topicName, int partitionCount, int brokers, int replication) {
		MutableMultimap<Integer, Integer> partitionDistribution =
				createPartitionDistribution(partitionCount, brokers, replication);
		ensureTopicCreated(zkClient, topicName, partitionCount, new Properties(),
				toKafkaPartitionMap(partitionDistribution));
	}

	public void deleteTopic(String topicName) {
		AdminUtils.deleteTopic(getKafkaRule().getZkClient(), topicName);
		if (getKafkaRule().isEmbedded()) {
			TestUtils.waitUntilMetadataIsPropagated(asScalaBuffer(getKafkaRule().getKafkaServers()), topicName, 0, 5000L);
		}
		else {
			sleep(1000);
		}
	}

	public MutableMultimap<Integer, Integer> createPartitionDistribution(int partitionCount, int brokers,
			int replication) {
		MutableMultimap<Integer, Integer> partitionDistribution = Multimaps.mutable.list.with();
		for (int i = 0; i < partitionCount; i++) {
			for (int j = 0; j < replication; j++) {
				partitionDistribution.put(i, (i + j) % brokers);
			}
		}
		return partitionDistribution;
	}


	public Configuration getKafkaConfiguration() {
		BrokerAddressListConfiguration configuration = new BrokerAddressListConfiguration(getKafkaRule().getBrokerAddresses());
		configuration.setSocketTimeout(500);
		return configuration;
	}

	public static Collection<ProducerRecord<String, String>> createMessages(int count, String topic, int partitionCount) {
		return createMessagesInRange(0, count - 1, topic, partitionCount);
	}

	public static Collection<ProducerRecord<String, String>> createMessagesInRange(int start, int end,
			String topic, int partitionCount) {
		List<ProducerRecord<String, String>> messages = new ArrayList<>();
		for (int i = start; i <= end; i++) {
			messages.add(new ProducerRecord<>(topic, i % partitionCount, "Key " + i, "Message " + i));
		}
		return messages;
	}

	public Sender<String, String> createMessageSender(String compression) {
		Properties producerConfig = new Properties();
		producerConfig.setProperty("bootstrap.servers", getKafkaRule().getBrokersAsString());
		producerConfig.setProperty("compression.type", compression);
		KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig,
				new EncoderAdaptingSerializer<>(new StringEncoder()),
				new EncoderAdaptingSerializer<>(new StringEncoder()));
		return new Sender<>(producer);
	}

	public ConnectionFactory getKafkaBrokerConnectionFactory() throws Exception {
		DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(getKafkaConfiguration());
		connectionFactory.afterPropertiesSet();
		return connectionFactory;
	}

	@SuppressWarnings({"rawtypes", "serial", "deprecation"})
	private Map toKafkaPartitionMap(Multimap<Integer, Integer> partitions) {
		java.util.Map<Object, Seq<Object>> m = partitions.toMap()
				.collect(new Function2<Integer, RichIterable<Integer>, Pair<Object, Seq<Object>>>() {

					@Override
					public Pair<Object, Seq<Object>> value(Integer argument1, RichIterable<Integer> argument2) {
						return Tuples.pair((Object) argument1,
								List$.MODULE$.fromArray(argument2.toArray(new Object[0])).toSeq());
					}

				});
		return Map$.MODULE$.apply(JavaConversions.asScalaMap(m).toSeq());
	}

	private static void sleep(int time) {
		try {
			Thread.sleep(time);
		}
		catch (InterruptedException e) {
			log.error(e);
		}
	}

	public class Sender<K,V> {

		private Producer<K,V> producer;

		public Sender(Producer<K, V> producer) {
			this.producer = producer;
		}

		public void send(Collection<ProducerRecord<K,V>> records) {
			Future<RecordMetadata> lastFuture = null;
			for (ProducerRecord<K, V> record : records) {
					lastFuture = producer.send(record);
			}
			// only block if there is at least one message to be sent
			if (lastFuture != null) {
				try {
					// block until the last message has been sent, so we make this deterministic
					lastFuture.get();
				} catch (InterruptedException e) {
					// not being able to confirm that all messages have been sent, fail the test
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
